package com.rohan.callnote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.rohan.callnote.utils.Constants;

/**
 * Created by Rohan on 08-Jan-17.
 */

public class CallStateReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {

            Intent i = new Intent(context, BaseCallNoteActivity.class);
            i.putExtra(Constants.ADD_NOTE_DIRECTLY_FROM_CALL, true);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }

    }
}
