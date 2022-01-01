package com.voxeldev.steammarkethelper.ui.misc;

import android.animation.Animator;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class DismissAnimatorListener implements Animator.AnimatorListener {

    private final BottomSheetDialogFragment fragment;
    private final String text;

    public DismissAnimatorListener(BottomSheetDialogFragment fragmentToDismiss,
                                   String textAfterDismiss) {
        fragment = fragmentToDismiss;
        text = textAfterDismiss;
    }

    @Override
    public void onAnimationStart(Animator animation) { }

    @Override
    public void onAnimationEnd(Animator animation) {
        Toast.makeText(fragment.requireContext(), text, Toast.LENGTH_LONG).show();
        fragment.dismiss();
    }

    @Override
    public void onAnimationCancel(Animator animation) { }

    @Override
    public void onAnimationRepeat(Animator animation) { }
}
