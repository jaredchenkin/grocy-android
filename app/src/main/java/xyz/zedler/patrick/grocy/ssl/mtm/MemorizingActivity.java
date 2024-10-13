package xyz.zedler.patrick.grocy.ssl.mtm;

import android.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.*;
import android.content.Intent;
import android.os.Bundle;
import xyz.zedler.patrick.grocy.R;

/**
 * Source: github.com/stephanritscher/MemorizingTrustManager
 */
public class MemorizingActivity extends Activity implements OnClickListener,OnCancelListener {

  private final static String TAG = MemorizingActivity.class.getSimpleName();

  int decisionId;

  AlertDialog dialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate:");
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();
    Intent i = getIntent();
    decisionId = i.getIntExtra(MemorizingTrustManager.DECISION_INTENT_ID, Decision.DECISION_INVALID);
    int titleId = i.getIntExtra(MemorizingTrustManager.DECISION_TITLE_ID, R.string.mtm_security_risk);
    String cert = i.getStringExtra(MemorizingTrustManager.DECISION_INTENT_CERT);
    Log.i(TAG,
        "onResume: with " + i.getExtras() + " decId=" + decisionId + " data: " + i.getData()
    );
    dialog = new AlertDialog.Builder(this).setTitle(titleId)
        .setMessage(cert)
        .setPositiveButton(R.string.mtm_decision_always, this)
        .setNeutralButton(R.string.mtm_decision_once, this)
        .setNegativeButton(R.string.action_cancel, this)
        .setOnCancelListener(this)
        .create();
    dialog.show();
  }

  @Override
  protected void onPause() {
    if (dialog.isShowing())
      dialog.dismiss();
    super.onPause();
  }

  void sendDecision(int decision) {
    Log.i(TAG, "sendDecision: " + decision);
    MemorizingTrustManager.interactResult(decisionId, decision);
    finish();
  }

  // react on AlertDialog button press
  public void onClick(DialogInterface dialog, int btnId) {
    int decision;
    dialog.dismiss();
    switch (btnId) {
      case DialogInterface.BUTTON_POSITIVE:
        decision = Decision.DECISION_ALWAYS;
        break;
      case DialogInterface.BUTTON_NEUTRAL:
        decision = Decision.DECISION_ONCE;
        break;
      default:
        decision = Decision.DECISION_ABORT;
    }
    sendDecision(decision);
  }

  public void onCancel(DialogInterface dialog) {
    sendDecision(Decision.DECISION_ABORT);
  }
}