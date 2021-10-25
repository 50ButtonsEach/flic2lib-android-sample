package flic.io.flic2androidsample;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import io.flic.flic2libandroid.Flic2Button;
import io.flic.flic2libandroid.Flic2ButtonListener;
import io.flic.flic2libandroid.Flic2Manager;

public class Flic2SampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // To prevent the application process from being killed while the app is running in the background, start a Foreground Service
        ContextCompat.startForegroundService(getApplicationContext(), new Intent(getApplicationContext(), Flic2SampleService.class));

        // Initialize the Flic2 manager to run on the same thread as the current thread (the main thread)
        Flic2Manager manager = Flic2Manager.initAndGetInstance(getApplicationContext(), new Handler());

        // Every time the app process starts, connect to all paired buttons and assign a click listener
        try {
            for (Flic2Button button : manager.getButtons()) {
                button.connect();
                listenToButtonWithToast(button);
            }
        } catch (SecurityException e) {
            // User has revoked the Bluetooth permissions for the app
            Toast.makeText(getApplicationContext(), "Bluetooth permissions have been revoked. Please re-enable for the app in system settings.", Toast.LENGTH_SHORT).show();
        }
    }

    public void listenToButtonWithToast(Flic2Button button) {
        button.addListener(new Flic2ButtonListener() {
            @Override
            public void onButtonUpOrDown(final Flic2Button button, boolean wasQueued, boolean lastQueued, long timestamp, boolean isUp, boolean isDown) {
                if (wasQueued && button.getReadyTimestamp() - timestamp > 15000) {
                    // Drop the event if it's more than 15 seconds old
                    return;
                }
                if (isDown) {
                    Toast.makeText(getApplicationContext(), "Button " + button + " was pressed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
