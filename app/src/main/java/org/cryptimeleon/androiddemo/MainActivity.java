package org.cryptimeleon.androiddemo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.cryptimeleon.math.structures.groups.Group;
import org.cryptimeleon.math.structures.groups.GroupElement;
import org.cryptimeleon.math.structures.groups.cartesian.GroupElementVector;
import org.cryptimeleon.math.structures.groups.elliptic.BilinearGroup;
import org.cryptimeleon.math.structures.groups.elliptic.BilinearMap;
import org.cryptimeleon.math.structures.groups.elliptic.type3.mcl.MclBilinearGroup;
import org.cryptimeleon.math.structures.rings.cartesian.RingElementVector;
import org.cryptimeleon.math.structures.rings.zn.Zn;

import java.math.BigInteger;

public class MainActivity extends AppCompatActivity {

    private TextView textViewResult;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load herumi/mcl for faster pairings
        System.loadLibrary("mcljava");

        textViewResult = findViewById(R.id.textview_result);
        button = findViewById(R.id.button_start);
        button.setOnClickListener(v -> computePairing());
    }

    /**
     * Setup, compute and verify a Pointcheval-Sanders signature.
     * Implements the pairing tutorial on https://cryptimeleon.github.io/getting-started/pairing-tutorial.html.
     * Sends ui updates to the UI thread since the computations are performed in a background thread.
     */
    private void computePairing() {
        button.setEnabled(false);
        textViewResult.setText("");

        AsyncTask.execute(() -> {
            appendToResultTextView("Starting pairing computation...");

            // Choose number of messages r
            int r = 3;

            // BN pairing is type 3 and we specify a 100 bit security parameter
            BilinearGroup bilinearGroup = new MclBilinearGroup();

            // Let's collect the values for our pp
            Group groupG1 = bilinearGroup.getG1();
            Group groupG2 = bilinearGroup.getG2();
            Group groupGT = bilinearGroup.getGT();
            BilinearMap e = bilinearGroup.getBilinearMap();
            BigInteger p = groupG1.size();
            Zn zp = bilinearGroup.getZn();
            appendToResultTextView("Generated bilinear group of order " + p);

            // Generate secret key
            Zn.ZnElement x = zp.getUniformlyRandomElement();
            RingElementVector y = zp.getUniformlyRandomElements(r); //computes a vector of r random numbers y_0, ..., y_(r-1)

            appendToResultTextView("x = " + x);
            appendToResultTextView("y = " + y);

            // Generate public key
            GroupElement tildeg = groupG2.getUniformlyRandomElement();
            GroupElement tildeX = tildeg.pow(x).precomputePow(); // this computes X = tildeg^x as above and runs precomputations to speed up later pow() calls on tildeX
            GroupElementVector tildeY = tildeg.pow(y).precomputePow(); // because y is a vector, this yields a vector of values tildeg.pow(y_0), tildeg.pow(y_1), ...
            appendToResultTextView("tildeg = " + tildeg);
            appendToResultTextView("tildeX = " + tildeX);
            appendToResultTextView("tildeY = " + tildeY);

            // Preparing messages ("Hello PS sigs", 42, 0, 0, ...)
            RingElementVector m = new RingElementVector(
                    bilinearGroup.getHashIntoZGroupExponent().hash("Hello PS sigs"),
                    zp.valueOf(42)).pad(zp.getZeroElement(), r
            );

            // Computing signature
            GroupElement sigma1 = groupG1.getUniformlyRandomNonNeutral().computeSync(); // h
            GroupElement sigma2 = sigma1.pow(x.add(y.innerProduct(m))).computeSync(); // h^{x + sum(y_i*m_i)}
            // The compute() call is optional but will cause sigma1 and sigma2 to be computed concurrently in the background.
            appendToResultTextView("sigma1 = " + sigma1);
            appendToResultTextView("sigma2 = " + sigma2);

            // Verify signature
            boolean signatureValid = !sigma1.isNeutralElement()
                    && e.apply(sigma1, tildeX.op(tildeY.innerProduct(m))).equals(e.apply(sigma2, tildeg));
            if (signatureValid) {
                appendToResultTextView("Signature valid!");
            } else {
                appendToResultTextView("Signature invalid!");
            }
            appendToResultTextView("Done!");

            runOnUiThread(() -> button.setEnabled(true));
        });
    }

    /**
     * Appends a string to the textViewResult textview and appends a line break.
     * Uses runOnUiThread since it is supposed to be used from a background thread.
     *
     * @param linesToAppend the string to add
     */
    private void appendToResultTextView(String linesToAppend) {
        runOnUiThread(() -> textViewResult.append(linesToAppend + "\n\n"));
    }
}