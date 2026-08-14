import re

with open("app/src/main/java/com/example/widget/ExpensesWidgetProvider.kt", "r") as f:
    content = f.read()

replacement = """                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_expenses)
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                        
                        views.setTextViewText(R.id.expenses_title, "💳 $accountLabel")
                        
                        if (isConfigured) {
                            views.setViewVisibility(R.id.btn_add_expense, android.view.View.VISIBLE)
                            views.setViewVisibility(R.id.expenses_balance, android.view.View.VISIBLE)
                            views.setOnClickPendingIntent(R.id.btn_add_expense, pendingActionIntent)
                            views.setTextViewText(R.id.btn_add_expense, buttonLabel)
                            views.setTextViewText(R.id.expenses_balance, "₹ $totalBalance")
                        } else {
                            views.setViewVisibility(R.id.btn_add_expense, android.view.View.GONE)
                            views.setViewVisibility(R.id.expenses_balance, android.view.View.GONE)
                        }
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }"""

content = re.sub(r'                withContext\(Dispatchers\.Main\) \{.*?appWidgetManager\.updateAppWidget\(appWidgetId, views\)\n                    \}\n                \}', replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/widget/ExpensesWidgetProvider.kt", "w") as f:
    f.write(content)
