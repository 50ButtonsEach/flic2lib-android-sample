package flic.io.flic2androidsample;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.flic.flic2libandroid.Flic2Button;
import io.flic.flic2libandroid.Flic2ButtonListener;
import io.flic.flic2libandroid.Flic2Manager;
import io.flic.flic2libandroid.Flic2ScanCallback;

public class MainActivity extends AppCompatActivity {
    private FlicRecyclerViewAdapter flicRecyclerViewAdapter = new FlicRecyclerViewAdapter();
    private boolean isScanning;

    static class FlicRecyclerViewAdapter extends RecyclerView.Adapter<FlicRecyclerViewAdapter.FlicViewHolder> {
        static class ButtonData {
            Flic2Button button;
            FlicViewHolder holder;
            boolean isDown;
            Flic2ButtonListener listener;

            public ButtonData(Flic2Button button) {
                this.button = button;
            }

            int getShapeColor() {
                switch (button.getConnectionState()) {
                    case Flic2Button.CONNECTION_STATE_CONNECTING:
                        return Color.RED;
                    case Flic2Button.CONNECTION_STATE_CONNECTED_STARTING:
                        return Color.YELLOW;
                    case Flic2Button.CONNECTION_STATE_CONNECTED_READY:
                        return isDown ? Color.BLUE : Color.GREEN;
                    default:
                        return Color.BLACK;
                }
            }
        }

        ArrayList<ButtonData> dataSet = new ArrayList<>();

        static class FlicViewHolder extends RecyclerView.ViewHolder {
            ButtonData buttonData;
            public LinearLayout linearLayout;
            public TextView bdaddrTxt;
            public Button connectBtn;
            public Button removeBtn;
            public LinearLayout circle;
            public FlicViewHolder(@NonNull LinearLayout itemView) {
                super(itemView);
                linearLayout = itemView;
                bdaddrTxt = itemView.findViewById(R.id.bdaddr);
                connectBtn = itemView.findViewById(R.id.button_connect);
                removeBtn = itemView.findViewById(R.id.button_remove);
                circle = itemView.findViewById(R.id.circle);
            }
        }

        @NonNull
        @Override
        public FlicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.flic_view, parent, false);
            return new FlicViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final FlicViewHolder holder, int position) {
            ButtonData buttonData = dataSet.get(position);
            holder.buttonData = buttonData;
            holder.buttonData.holder = holder;
            holder.bdaddrTxt.setText(buttonData.button.getBdAddr());
            holder.connectBtn.setText(buttonData.button.getConnectionState() == Flic2Button.CONNECTION_STATE_DISCONNECTED ? "Connect" : "Disconnect");
            holder.circle.getBackground().setColorFilter(new PorterDuffColorFilter(holder.buttonData.getShapeColor(), PorterDuff.Mode.SRC_ATOP));
            holder.connectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.buttonData.button.getConnectionState() == Flic2Button.CONNECTION_STATE_DISCONNECTED) {
                        try {
                            holder.buttonData.button.connect();
                            holder.connectBtn.setText("Disconnect");
                        } catch (SecurityException e) {
                            Toast.makeText(holder.connectBtn.getContext(), "Bluetooth permissions have been revoked. Please re-enable for the app in system settings.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        holder.buttonData.button.disconnectOrAbortPendingConnection();
                        holder.connectBtn.setText("Connect");
                    }
                    holder.circle.getBackground().setColorFilter(new PorterDuffColorFilter(holder.buttonData.getShapeColor(), PorterDuff.Mode.SRC_ATOP));
                }
            });
            holder.removeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Flic2Manager.getInstance().forgetButton(holder.buttonData.button);
                }
            });
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }

        public void onDestroy() {
            for (ButtonData data : dataSet) {
                data.button.removeListener(data.listener);
            }
        }

        public void addButton(Flic2Button button) {
            final ButtonData buttonData = new ButtonData(button);

            buttonData.listener = new Flic2ButtonListener() {
                FlicViewHolder getHolder() {
                    if (buttonData.holder != null && buttonData.holder.buttonData == buttonData) {
                        return buttonData.holder;
                    }
                    return null;
                }

                private void updateColor() {
                    FlicViewHolder holder = getHolder();
                    if (holder != null) {
                        holder.circle.getBackground().setColorFilter(new PorterDuffColorFilter(holder.buttonData.getShapeColor(), PorterDuff.Mode.SRC_ATOP));
                    }
                }

                @Override
                public void onButtonUpOrDown(Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isUp, boolean isDown) {
                    buttonData.isDown = isDown;
                    updateColor();
                }

                @Override
                public void onConnect(Flic2Button button) {
                    updateColor();
                }

                @Override
                public void onReady(Flic2Button button, long timestamp) {
                    updateColor();
                }

                @Override
                public void onDisconnect(Flic2Button button) {
                    updateColor();
                }

                @Override
                public void onUnpaired(Flic2Button button) {
                    int index = -1;
                    for (int i = 0; i < dataSet.size(); i++) {
                        if (dataSet.get(i).button == button) {
                            index = i;
                            break;
                        }
                    }
                    if (index != -1) {
                        dataSet.remove(index);
                        notifyItemRemoved(index);
                    }
                }
            };
            button.addListener(buttonData.listener);

            dataSet.add(buttonData);
            notifyItemInserted(dataSet.size() - 1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.flicsView);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setAdapter(flicRecyclerViewAdapter);

        for (Flic2Button button : Flic2Manager.getInstance().getButtons()) {
            flicRecyclerViewAdapter.addButton(button);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // This will make sure button listeners are correctly removed
        flicRecyclerViewAdapter.onDestroy();

        // Stop a scan, if it's running
        Flic2Manager.getInstance().stopScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT < 31 || getApplicationInfo().targetSdkVersion < 31) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanNewButton(findViewById(R.id.scanNewButton));
                } else {
                    Toast.makeText(getApplicationContext(), "Scanning needs Location permission, which you have rejected", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (grantResults.length >= 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    scanNewButton(findViewById(R.id.scanNewButton));
                } else {
                    Toast.makeText(getApplicationContext(), "Scanning needs permissions for finding nearby devices, which you have rejected", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @TargetApi(31)
    public void scanNewButton(View v) {
        if (isScanning) {
            Flic2Manager.getInstance().stopScan();
            isScanning = false;

            ((Button) findViewById(R.id.scanNewButton)).setText("Scan new button");
            ((TextView) findViewById(R.id.scanWizardStatus)).setText("");
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT < 31 || getApplicationInfo().targetSdkVersion < 31) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                        return;
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 1);
                        return;
                    }
                }
            }

            ((Button) findViewById(R.id.scanNewButton)).setText("Cancel scan");
            ((TextView) findViewById(R.id.scanWizardStatus)).setText("Press and hold down your Flic2 button until it connects");

            isScanning = true;

            Flic2Manager.getInstance().startScan(new Flic2ScanCallback() {
                @Override
                public void onDiscoveredAlreadyPairedButton(Flic2Button button) {
                    ((TextView) findViewById(R.id.scanWizardStatus)).setText("Found an already paired button. Try another button.");
                }

                @Override
                public void onDiscovered(String bdAddr) {
                    ((TextView) findViewById(R.id.scanWizardStatus)).setText("Found Flic2, now connecting...");
                }

                @Override
                public void onConnected() {
                    ((TextView) findViewById(R.id.scanWizardStatus)).setText("Connected. Now pairing...");
                }

                @Override
                public void onAskToAcceptPairRequest() {
                    ((TextView) findViewById(R.id.scanWizardStatus)).setText("Please press \"Pair & Connect\" in the system dialog...");
                }

                @Override
                public void onComplete(int result, int subCode, Flic2Button button) {
                    isScanning = false;

                    ((Button)findViewById(R.id.scanNewButton)).setText("Scan new button");

                    if (result == Flic2ScanCallback.RESULT_SUCCESS) {
                        ((TextView) findViewById(R.id.scanWizardStatus)).setText("Scan wizard success!");
                        ((Flic2SampleApplication) getApplication()).listenToButtonWithToast(button);
                        flicRecyclerViewAdapter.addButton(button);
                    } else {
                        ((TextView) findViewById(R.id.scanWizardStatus)).setText("Scan wizard failed with code " + Flic2Manager.errorCodeToString(result));
                    }
                }
            });
        }
    }
}
