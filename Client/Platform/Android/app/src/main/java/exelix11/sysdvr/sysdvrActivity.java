package exelix11.sysdvr;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import org.libsdl.app.SDLActivity;

public class sysdvrActivity extends SDLActivity
{
    public static sysdvrActivity instance;

    // Flag so the C# layer knows it should skip the home screen and auto-connect USB
    public static boolean autoStartUsb = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log("SysDVRActivity onCreate()");
        super.onCreate(savedInstanceState);
        instance = this;
        CheckPackageName();

        // Check if we were launched by a USB_DEVICE_ATTACHED event
        handleLaunchIntent(getIntent());

        Log("SysDVRActivity created, autoStartUsb=" + autoStartUsb);
    }

    // Called when the app is already running and a new USB device is plugged in
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log("SysDVRActivity onNewIntent()");
        handleLaunchIntent(intent);
    }

    // Detect if this launch was triggered by the Switch being plugged in via USB
    private void handleLaunchIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        Log("SysDVRActivity handleLaunchIntent action=" + action);

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null) {
                int vid = device.getVendorId();
                int pid = device.getProductId();
                Log("USB device attached: VID=" + vid + " PID=" + pid);

                // VID 0x18D1 = Google (ADB), PID 0x4EE0 = SysDVR USB mode
                if (vid == 0x18D1 && pid == 0x4EE0) {
                    Log("Switch with SysDVR detected - enabling auto USB connect");
                    autoStartUsb = true;
                }
            }
        }
    }

    // Called from native C# layer via JNI to check if we should auto-start USB
    public static boolean ShouldAutoStartUsb() {
        return autoStartUsb;
    }

    // Called from native C# layer after auto-connect is initiated, to reset the flag
    public static void ClearAutoStartUsb() {
        autoStartUsb = false;
    }

    static boolean checkOnce = true;
    void CheckPackageName() {
        /*
        * I'm not really into the android world but apparently people reuploading existing apps with ads to the store is a thing.
        * i'm conflicted about fighting this as it would go against the open source nature of the project.
        * So for now i'll just have a slightly obfuscated check here, if you're just making a fork feel free to remove this.
        * My only condition is that you don't upload this to the play store.
        */
        if (!checkOnce)
            return;

        checkOnce = false;

        if (getPackageName().equals("exelix" + ((Integer)11).toString() + getString(R.string.hello_txt).charAt(3) + "sysdvr"))
            return;

        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage("You're using a SyDVR-Client version that was not downloaded from the official GitHub repository. This is at your own risk.");
        dlgAlert.setTitle("Warning");
        dlgAlert.setPositiveButton("Dismiss",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        dlgAlert.setNeutralButton("Open GitHub page", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SystemHelper.OpenURL("https://github.com/exelix11/SysDVR");
            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    public static void Log(String message) {
        Log.i("SysDVRJava", message);
    }
}

