package com.utopia.robust;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import com.meituan.robust.PatchExecutor;
import com.utopia.activity.LocalPatchExecutor;
import com.utopia.activity.LocalPatchManipulateImp;
import com.utopia.activity.PatchManipulateImp;
import com.utopia.activity.RobustCallBackSample;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.tv_content)
        .setOnClickListener(
            v -> {
              Test test = new Test();
              ((TextView) v).setText(test.getMessage("---"));
            });

    findViewById(R.id.btn_fixed)
        .setOnClickListener(
            v -> {
              runRobust();
            });
  }

  private void runLocalRobust() {
    new LocalPatchExecutor(
            getApplicationContext(), new LocalPatchManipulateImp(), new RobustCallBackSample())
        .start();
  }

  private void runRobust() {
    Context c = getApplicationContext();
    new PatchExecutor(new PatchManipulateImp(c), new RobustCallBackSample()).start();
  }
}
