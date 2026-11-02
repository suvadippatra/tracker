#!/bin/bash
perl -pi -e 's/it\.navigationBarColor = android\.graphics\.Color\.TRANSPARENT/it.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT)\n            it.navigationBarColor = android.graphics.Color.TRANSPARENT/g' app/src/main/java/com/example/ui/components/DialogEdgeToEdge.kt
