import re

with open("app/src/main/java/com/example/widget/OverallWidgetProvider.kt", "r") as f:
    content = f.read()

replacement = """                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_overall)
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                        
                        views.setTextViewText(R.id.overall_title, titleLabel)
                        
                        if (isHotelConfigured || isAccountConfigured) {
                            views.setViewVisibility(R.id.overall_net, android.view.View.VISIBLE)
                            views.setTextViewText(R.id.overall_net, netLabel)
                        } else {
                            views.setViewVisibility(R.id.overall_net, android.view.View.GONE)
                        }
                        
                        if (isHotelConfigured) {
                            views.setViewVisibility(R.id.btn_add_meal, android.view.View.VISIBLE)
                            views.setOnClickPendingIntent(R.id.btn_add_meal, pendingHotelIntent)
                            views.setTextViewText(R.id.btn_add_meal, hotelBtnLabel)
                        } else {
                            views.setViewVisibility(R.id.btn_add_meal, android.view.View.GONE)
                        }
                        
                        if (isAccountConfigured) {
                            views.setViewVisibility(R.id.btn_add_expense, android.view.View.VISIBLE)
                            views.setOnClickPendingIntent(R.id.btn_add_expense, pendingExpenseIntent)
                            views.setTextViewText(R.id.btn_add_expense, expenseBtnLabel)
                        } else {
                            views.setViewVisibility(R.id.btn_add_expense, android.view.View.GONE)
                        }
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }"""

content = re.sub(r'                withContext\(Dispatchers\.Main\) \{.*?appWidgetManager\.updateAppWidget\(appWidgetId, views\)\n                    \}\n                \}', replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/widget/OverallWidgetProvider.kt", "w") as f:
    f.write(content)
