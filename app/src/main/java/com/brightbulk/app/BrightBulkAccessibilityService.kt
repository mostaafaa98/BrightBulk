package com.brightbulk.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class BrightBulkAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // مساعد للتنقل ومراقبة الشاشة فقط.
        // لا ينفذ ضغطًا تلقائيًا على زر Send داخل تطبيقات المراسلة.
    }

    override fun onInterrupt() {}
}
