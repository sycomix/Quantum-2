package hu.hexadecimal.quantum;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;
import hu.hexadecimal.quantum.graphics.QuantumView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class UIHelper {

    private static final double[] importantAngles = new double[]{0, Math.PI / 8, Math.PI / 6, Math.PI / 4, Math.PI / 3, Math.PI / 2, Math.PI / 3 * 2, Math.PI / 4 * 3, Math.PI};
    private static final String[] importantAngleNames = new String[]{"0", "π/8", "π/6", "π/4", "π/3", "π/2", "2π/3", "3π/4", "π"};

    private static final double[] importantAngles2PI = new double[]{0, Math.PI / 8, Math.PI / 6, Math.PI / 4, Math.PI / 3, Math.PI / 2, Math.PI / 3 * 2, Math.PI / 4 * 3, Math.PI,
            Math.PI / 5 * 4, Math.PI / 3 * 4, Math.PI / 2 * 3, Math.PI / 3 * 5, Math.PI / 4 * 7};
    private static final String[] importantAngleNames2PI = new String[]{"0", "π/8", "π/6", "π/4", "π/3", "π/2", "2π/3", "3π/4", "π",
            "5π/4", "4π/3", "3π/2", "5π/3", "7π/4"};

    public void runnableForGateSelection(AppCompatActivity context, QuantumView qv, VisualOperator prevOperator, float posx, float posy, Dialog d) {
        final LinkedList<VisualOperator> operators = new LinkedList<>();
        final LinkedList<String> operatorNames = new LinkedList<>();
        new Thread(() -> {
            try {
                Uri uri = context.getContentResolver().getPersistedUriPermissions().get(0).getUri();
                DocumentFile pickedDir = DocumentFile.fromTreeUri(context, uri);
                if (!pickedDir.exists()) {
                    context.getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    pickedDir = null;
                }
                synchronized (UIHelper.this) {
                    for (DocumentFile file : pickedDir.listFiles()) {
                        try {
                            if (file.getName().endsWith(VisualOperator.FILE_EXTENSION_LEGACY)) {
                                ObjectInputStream oi = new ObjectInputStream(context.getContentResolver().openInputStream(file.getUri()));
                                VisualOperator m = (VisualOperator) oi.readObject();
                                oi.close();
                                operatorNames.add(m.getName());
                                operators.add(m);
                            } else if (file.getName().endsWith(VisualOperator.FILE_EXTENSION)) {
                                BufferedReader in = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(file.getUri())));
                                StringBuilder total = new StringBuilder();
                                for (String line; (line = in.readLine()) != null; ) {
                                    total.append(line).append('\n');
                                }
                                String json = total.toString();
                                VisualOperator m = VisualOperator.fromJSON(new JSONObject(json));
                                operatorNames.add(m.getName());
                                operators.add(m);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IndexOutOfBoundsException ie) {
                Log.i("GateAdder", "Probably no home selected");
            } catch (Exception e) {
                Log.e("GateAdder", "Some error has happened :(");
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            Looper.prepare();
            final View mainView = context.getLayoutInflater().inflate(R.layout.gate_selector, null);
            final Spinner gateType = mainView.findViewById(R.id.type_spinner);
            final Spinner filter = mainView.findViewById(R.id.filter_spinner);
            final Spinner gateName = mainView.findViewById(R.id.gate_name_spinner);
            final ConstraintLayout subLayout = mainView.findViewById(R.id.sub_layout);
            final ConstraintLayout rotLayout = mainView.findViewById(R.id.rot_layout);
            final SeekBar thetaBar = mainView.findViewById(R.id.rx);
            final SeekBar phiBar = mainView.findViewById(R.id.rz);
            final SeekBar lamdaBar = mainView.findViewById(R.id.ry);
            final TextView thetaText = mainView.findViewById(R.id.rx_text);
            final TextView phiText = mainView.findViewById(R.id.rz_text);
            final TextView lambdaText = mainView.findViewById(R.id.ry_text);
            final CheckBox fixedValues = mainView.findViewById(R.id.fixed_values);
            final SeekBar[] qX = new SeekBar[]{
                    mainView.findViewById(R.id.order_first),
                    mainView.findViewById(R.id.order_second),
                    mainView.findViewById(R.id.order_third),
                    mainView.findViewById(R.id.order_fourth)};
            final TextView[] tX = new TextView[]{
                    mainView.findViewById(R.id.qtext1),
                    mainView.findViewById(R.id.qtext2),
                    mainView.findViewById(R.id.qtext3),
                    mainView.findViewById(R.id.qtext4)};
            final LinkedList<String> mGates = VisualOperator.getPredefinedGateNames();
            Collections.sort(mGates);
            ArrayAdapter<String> gateAdapter =
                    new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, mGates);

            gateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            context.runOnUiThread(() -> {
                gateName.setAdapter(gateAdapter);
                for (int i = 0; i < qX.length; i++) {
                    final int loop_pos = i;
                    qX[i].setMax(qv.getDisplayedQubits() - 1);
                    qX[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int j, boolean b) {
                            tX[loop_pos].setText("q" + (j + 1));
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
                }
                if (prevOperator != null) {
                    if (!prevOperator.isRotation() && !prevOperator.isU3()) {
                        for (int i = 0; i < prevOperator.getQubitIDs().length; i++) {
                            qX[i].setProgress(prevOperator.getQubitIDs()[i]);
                            tX[i].setText("q" + (prevOperator.getQubitIDs()[i] + 1));
                            int pos = mGates.indexOf(prevOperator.getName());
                            if (pos >= 0) {
                                gateName.setSelection(pos);
                            } else {
                                gateName.setSelection(mGates.indexOf(VisualOperator.HADAMARD.getName()));
                            }
                        }
                        switch (prevOperator.getQubitIDs().length) {
                            case 4:
                                qX[3].setVisibility(View.VISIBLE);
                                tX[3].setVisibility(View.VISIBLE);
                            case 3:
                                qX[2].setVisibility(View.VISIBLE);
                                tX[2].setVisibility(View.VISIBLE);
                            case 2:
                                qX[1].setVisibility(View.VISIBLE);
                                tX[1].setVisibility(View.VISIBLE);
                            default:
                        }
                        switch (prevOperator.getQubitIDs().length) {
                            case 1:
                                qX[1].setVisibility(View.INVISIBLE);
                                tX[1].setVisibility(View.INVISIBLE);
                            case 2:
                                qX[2].setVisibility(GONE);
                                tX[2].setVisibility(GONE);
                            case 3:
                                qX[3].setVisibility(GONE);
                                tX[3].setVisibility(GONE);
                            default:
                        }
                    }
                } else {
                    int which = qv.whichQubit(posy);
                    qX[0].setProgress(which);
                    tX[0].setText("q" + (which + 1));
                }
                gateType.post(() ->
                        gateType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                if (i == 0) {
                                    filter.setEnabled(true);
                                    subLayout.setVisibility(VISIBLE);
                                    rotLayout.setVisibility(GONE);
                                    if (filter.getSelectedItemPosition() == 0) {
                                        ArrayAdapter<String> gateAdapter =
                                                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, mGates);

                                        gateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        gateName.setAdapter(gateAdapter);
                                    } else {
                                        LinkedList<String> mGates = VisualOperator.getPredefinedGateNames(filter.getSelectedItemPosition() == 1);
                                        Collections.sort(mGates);
                                        ArrayAdapter<String> gateAdapter =
                                                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, mGates);

                                        gateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        gateName.setAdapter(gateAdapter);
                                    }
                                } else if (i == 1) {
                                    synchronized (UIHelper.this) {
                                        if (operators.size() > 0) {
                                            filter.setEnabled(false);
                                            filter.setSelection(0);
                                            subLayout.setVisibility(VISIBLE);
                                            rotLayout.setVisibility(GONE);
                                            ArrayAdapter<String> gateAdapter =
                                                    new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, operatorNames);
                                            gateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                            gateName.setAdapter(gateAdapter);
                                        } else {
                                            gateType.setSelection(0);
                                            Toast t = Toast.makeText(context, R.string.no_user_gates, Toast.LENGTH_SHORT);
                                            t.setGravity(Gravity.CENTER, 0, 0);
                                            t.show();
                                        }
                                    }
                                } else if (i == 2 || i == 3) {
                                    for (int k = 1; k < qX.length; k++) {
                                        qX[k].setVisibility(GONE);
                                        tX[k].setVisibility(GONE);
                                    }
                                    if (i == 2) {
                                        lambdaText.setVisibility(GONE);
                                        lamdaBar.setVisibility(GONE);
                                    } else {
                                        lambdaText.setVisibility(VISIBLE);
                                        lamdaBar.setVisibility(VISIBLE);
                                    }
                                    subLayout.setVisibility(GONE);
                                    rotLayout.setVisibility(VISIBLE);
                                    filter.setSelection(0);
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {
                            }
                        }));
                filter.post(() ->
                        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                if (gateType.getSelectedItemPosition() == 0) {
                                    if (i == 0) {
                                        ArrayAdapter<String> gateAdapter =
                                                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, mGates);

                                        gateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        gateName.setAdapter(gateAdapter);
                                    } else {
                                        LinkedList<String> mGates = VisualOperator.getPredefinedGateNames(i == 1);
                                        Collections.sort(mGates);
                                        ArrayAdapter<String> gateAdapter =
                                                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, mGates);

                                        gateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        gateName.setAdapter(gateAdapter);
                                    }
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        }));
                mainView.postDelayed(() -> {
                    fixedValues.setOnCheckedChangeListener((CompoundButton compoundButton, boolean b) ->
                            new Handler().postDelayed(() -> {
                                if (b) {
                                    thetaBar.setMax(importantAngles.length - 1);
                                    lamdaBar.setMax(importantAngles2PI.length - 1);
                                    phiBar.setMax(importantAngles2PI.length - 1);
                                    thetaBar.setProgress(0);
                                    phiBar.setProgress(0);
                                    lamdaBar.setProgress(0);
                                } else {
                                    thetaBar.setMax(3141);
                                    lamdaBar.setMax(6282);
                                    phiBar.setMax(6282);
                                    thetaBar.setProgress(0);
                                    phiBar.setProgress(0);
                                    lamdaBar.setProgress(0);
                                }
                            }, 100));
                    DecimalFormat df = new DecimalFormat("0.000");
                    thetaBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            if (fixedValues.isChecked()) {
                                thetaText.setText(String.format("\u03B8 %-4s", importantAngleNames[i]));
                            } else {
                                thetaText.setText("\u03B8 " + df.format(i / 1000f));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    });
                    lamdaBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            if (fixedValues.isChecked()) {
                                lambdaText.setText(String.format("\u03BB %-4s", importantAngleNames2PI[i]));
                            } else {
                                lambdaText.setText("\u03BB " + df.format(i / 1000f));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    });
                    phiBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                            if (fixedValues.isChecked()) {
                                phiText.setText(String.format("\u03C6 %-4s", importantAngleNames2PI[i]));
                            } else {
                                phiText.setText("\u03C6 " + df.format(i / 1000f));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    });
                    if (prevOperator != null && prevOperator.isRotation()) {
                        gateType.setSelection(2);
                        qX[0].setProgress(prevOperator.getQubitIDs()[0]);
                        phiBar.setMax(6282);
                        thetaBar.setProgress((int) Math.abs(prevOperator.getAngles()[0] * 1000));
                        phiBar.setProgress((int) Math.abs(prevOperator.getAngles()[1] * 1000));
                        if (prevOperator.getAngles()[0] < 0 || prevOperator.getAngles()[1] < 0) {
                            ((CheckBox) mainView.findViewById(R.id.hermitianConjugate)).setChecked(true);
                        }
                    } else if (prevOperator != null && prevOperator.isU3()) {
                        gateType.setSelection(3);
                        qX[0].setProgress(prevOperator.getQubitIDs()[0]);
                        thetaBar.setProgress((int) Math.abs(prevOperator.getAngles()[0] * 1000));
                        lamdaBar.setProgress((int) Math.abs(prevOperator.getAngles()[2] * 1000));
                        phiBar.setProgress((int) Math.abs(prevOperator.getAngles()[1] * 1000));
                        if (prevOperator.getAngles()[0] < 0 || prevOperator.getAngles()[1] < 0 || prevOperator.getAngles()[2] < 0) {
                            ((CheckBox) mainView.findViewById(R.id.hermitianConjugate)).setChecked(true);
                        }
                    } else {
                        subLayout.setVisibility(VISIBLE);
                    }
                    gateName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            LinkedList<String> gates = VisualOperator.getPredefinedGateNames(filter.getSelectedItemPosition() == 1);
                            Collections.sort(gates);
                            ArrayAdapter<String> adapter =
                                    new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, filter.getSelectedItemPosition() == 0 ? mGates : gates);

                            synchronized (UIHelper.this) {
                                if (gateType.getSelectedItemPosition() == 1 && operators.size() == 0) {
                                    gateType.setSelection(0);
                                    Toast t = Toast.makeText(context, R.string.no_user_gates, Toast.LENGTH_SHORT);
                                    t.setGravity(Gravity.CENTER, 0, 0);
                                    t.show();
                                    return;
                                } else if (gateType.getSelectedItemPosition() == 1 && operators.size() - 1 < i) {
                                    Log.e("Unknown error", "Gate name index is unacceptably large");
                                    gateName.setSelection(0);
                                }

                                int qubits = gateType.getSelectedItemPosition() == 0 ?
                                        VisualOperator.findGateByName(adapter.getItem(i)).getQubits() :
                                        gateType.getSelectedItemPosition() == 1 ?
                                                operators.get(i).getQubits() : 1;
                                switch (qubits) {
                                    case 4:
                                        qX[3].setVisibility(View.VISIBLE);
                                        tX[3].setVisibility(View.VISIBLE);
                                    case 3:
                                        qX[2].setVisibility(View.VISIBLE);
                                        tX[2].setVisibility(View.VISIBLE);
                                    case 2:
                                        qX[1].setVisibility(View.VISIBLE);
                                        tX[1].setVisibility(View.VISIBLE);
                                    default:
                                }
                                switch (qubits) {
                                    case 1:
                                        qX[1].setVisibility(View.INVISIBLE);
                                        tX[1].setVisibility(View.INVISIBLE);
                                    case 2:
                                        qX[2].setVisibility(GONE);
                                        tX[2].setVisibility(GONE);
                                    case 3:
                                        qX[3].setVisibility(GONE);
                                        tX[3].setVisibility(GONE);
                                    default:
                                }
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                        }
                    });
                    mainView.findViewById(R.id.cancel).setOnClickListener((View view) -> d.cancel());
                    mainView.findViewById(R.id.ok).setOnClickListener((View view) -> {
                        if (gateType.getSelectedItemPosition() < 2) {
                            synchronized (UIHelper.this) {
                                if (operators.size() == 0 && gateType.getSelectedItemPosition() != 0) {
                                    return;
                                }
                            }
                            LinkedList<String> gates = VisualOperator.getPredefinedGateNames(filter.getSelectedItemPosition() == 1);
                            Collections.sort(gates);
                            ArrayAdapter<String> adapter =
                                    new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, filter.getSelectedItemPosition() == 0 ? mGates : gates);

                            synchronized (UIHelper.this) {
                                if (gateType.getSelectedItemPosition() == 1 && operators.size() == 0) {
                                    gateType.setSelection(0);
                                    Toast t = Toast.makeText(context, R.string.no_user_gates, Toast.LENGTH_SHORT);
                                    t.setGravity(Gravity.CENTER, 0, 0);
                                    t.show();
                                    return;
                                } else if (gateType.getSelectedItemPosition() == 1 && operators.size() - 1 < gateName.getSelectedItemPosition()) {
                                    Log.e("Unknown error", "Gate name index is unacceptably large");
                                    gateName.setSelection(0);
                                }

                                int qubits = gateType.getSelectedItemPosition() == 0 ?
                                        VisualOperator.findGateByName(adapter.getItem(gateName.getSelectedItemPosition())).getQubits() :
                                        gateType.getSelectedItemPosition() == 1 ?
                                                operators.get(gateName.getSelectedItemPosition()).getQubits() : 1;

                                int[] quids = new int[qubits];
                                for (int i = 0; i < qubits; i++) {
                                    quids[i] = qX[i].getProgress();
                                }
                                for (int i = 0; i < qubits; i++) {
                                    for (int j = i + 1; j < qubits; j++) {
                                        if (quids[i] == quids[j]) {
                                            d.cancel();
                                            Snackbar snackbar = Snackbar.make(context.findViewById(R.id.parent2), R.string.use_different_qubits, Snackbar.LENGTH_LONG);
                                            snackbar.getView().setBackgroundColor(0xffD81010);
                                            snackbar.show();
                                            return;
                                        }
                                    }
                                }
                                //TODO saved = false;
                                VisualOperator gate = gateType.getSelectedItemPosition() == 0
                                        ? VisualOperator.findGateByName((String) gateName.getSelectedItem()).copy()
                                        : operators.get(gateName.getSelectedItemPosition()).copy();
                                if (((CheckBox) mainView.findViewById(R.id.hermitianConjugate)).isChecked())
                                    gate.hermitianConjugate();
                                if (prevOperator == null)
                                    qv.addGate(quids, gate);
                                else
                                    qv.replaceGateAt(quids, gate, posx, posy);
                            }
                            d.cancel();
                        } else if (gateType.getSelectedItemPosition() == 2) {
                            double theta = fixedValues.isChecked() ? importantAngles[thetaBar.getProgress()] : thetaBar.getProgress() / 1000f;
                            double phi = fixedValues.isChecked() ? importantAngles2PI[phiBar.getProgress()] : phiBar.getProgress() / 1000f;
                            VisualOperator gate = new VisualOperator(theta, phi);
                            if (((CheckBox) mainView.findViewById(R.id.hermitianConjugate)).isChecked())
                                gate.hermitianConjugate();
                            if (prevOperator == null)
                                qv.addGate(new int[]{qX[0].getProgress()}, gate);
                            else
                                qv.replaceGateAt(new int[]{qX[0].getProgress()}, gate, posx, posy);
                            d.cancel();
                        } else if (gateType.getSelectedItemPosition() == 3) {
                            double theta = fixedValues.isChecked() ? importantAngles[thetaBar.getProgress()] : thetaBar.getProgress() / 1000f;
                            double phi = fixedValues.isChecked() ? importantAngles2PI[phiBar.getProgress()] : phiBar.getProgress() / 1000f;
                            double lambda = fixedValues.isChecked() ? importantAngles2PI[lamdaBar.getProgress()] : lamdaBar.getProgress() / 1000f;
                            VisualOperator gate = new VisualOperator(theta, phi, lambda);
                            if (((CheckBox) mainView.findViewById(R.id.hermitianConjugate)).isChecked())
                                gate.hermitianConjugate();
                            if (prevOperator == null)
                                qv.addGate(new int[]{qX[0].getProgress()}, gate);
                            else
                                qv.replaceGateAt(new int[]{qX[0].getProgress()}, gate, posx, posy);
                            d.cancel();
                        }
                    });
                    mainView.findViewById(R.id.gate_selector_main).setOnClickListener((View v) -> d.cancel());
                }, 100);
                d.setTitle(R.string.select_gate);
                try {
                    d.setContentView(mainView);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }

    public void applyActions(AppCompatActivity context, QuantumView qv, VisualOperator prevOperator, float posx, float posy, Dialog d, View layout) {
        layout.findViewById(R.id.delete_selected_gate).setOnClickListener((View view) -> {
            qv.deleteGateAt(posx, posy);
            d.cancel();
        });
        layout.findViewById(R.id.edit_selected_gate).setOnClickListener((View view) -> {
            this.runnableForGateSelection(context, qv, prevOperator, posx, posy, d);
        });
        layout.findViewById(R.id.move_selected_gate_left).setOnClickListener((View view) -> {
            qv.moveGate(posx, posy, false);
            d.cancel();
        });
        layout.findViewById(R.id.move_selected_gate_left).setOnLongClickListener((View view) -> {
            Toast.makeText(context, R.string.move_gate_to_left, Toast.LENGTH_SHORT).show();
            return true;
        });
        layout.findViewById(R.id.move_selected_gate_right).setOnClickListener((View view) -> {
            qv.moveGate(posx, posy, true);
            d.cancel();
        });
        layout.findViewById(R.id.move_selected_gate_right).setOnLongClickListener((View view) -> {
            Toast.makeText(context, R.string.move_gate_to_right, Toast.LENGTH_SHORT).show();
            return true;
        });
        layout.findViewById(R.id.gate_action_main).setOnClickListener((View view) -> d.cancel());
    }
}
