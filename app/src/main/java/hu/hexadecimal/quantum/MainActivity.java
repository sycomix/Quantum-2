package hu.hexadecimal.quantum;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import hu.hexadecimal.quantum.graphics.BlochSphereRenderer;
import hu.hexadecimal.quantum.graphics.BlochSphereView;

public class MainActivity extends Activity {

    // Used to load the 'native-lib' library on application startup.
    /*static {
        System.loadLibrary("native-lib");
    }*/

    BlochSphereView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        TextView tv = (TextView) findViewById(R.id.sample_text);
        glSurfaceView = new BlochSphereView(this);

        //tv.setText(tv.getText() + "\n---\n" + Complex.multiply(new Complex(0), new Complex(0)).toString3Decimals());
        //tv.setText(new Complex(0).arg() + " -- " + new Complex(0).mod());

        QBit q = new QBit();
        q.prepare(true);
        tv.setText(tv.getText() + "" + q.measureZ() + "");
        q.applyOperator(LinearOperator.HADAMARD);
        //tv.setText(tv.getText() + "\n--\n" + q.toString());
        //tv.setText(tv.getText() + "\n--\n" + LinearOperator.HADAMARD.operateOn(q).toString());

        double value = 0;

        for (int i = 0; i < 200; i++) {
            QBit[] qs = TwoQBitOperator.CNOT.operateOn(q, q);
            value += qs[1].measureZ() ? 1 : 0;
        }
        tv.setText(tv.getText() + "\n----\n" + value / 200);
        List<String> gates = GateView.getPredefinedGateNames();
        for (String s : gates) {
            Log.d("?", s);
        }
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        if (configurationInfo.reqGlEsVersion >= 0x20000) {
            displayBlochSphere(q);
        } else {
            Toast.makeText(MainActivity.this, R.string.gl_version_not_supported, Toast.LENGTH_LONG).show();
            return;
        }

    }

    //public native String stringFromJNI();

    public void displayBlochSphere(QBit q) {

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.bloch_sphere);
        adb.setPositiveButton("OK", null);
        adb.setView(glSurfaceView);
        adb.show();
    }
}
