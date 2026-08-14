import re

with open("app/src/main/java/com/example/widget/HotelWidgetProvider.kt", "r") as f:
    content = f.read()

replacement = """                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_hotel)
                        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                        
                        views.setTextViewText(R.id.hotel_name, "🏨 $hotelName")
                        
                        if (isConfigured) {
                            views.setViewVisibility(R.id.btn_add_meal, android.view.View.VISIBLE)
                            views.setViewVisibility(R.id.hotel_total, android.view.View.VISIBLE)
                            views.setOnClickPendingIntent(R.id.btn_add_meal, pendingActionIntent)
                            views.setTextViewText(R.id.btn_add_meal, buttonLabel)
                            views.setTextViewText(R.id.hotel_total, "₹ $total")
                        } else {
                            views.setViewVisibility(R.id.btn_add_meal, android.view.View.GONE)
                            views.setViewVisibility(R.id.hotel_total, android.view.View.GONE)
                        }
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }"""

# Use regex to replace the withContext block
content = re.sub(r'                withContext\(Dispatchers\.Main\) \{.*?appWidgetManager\.updateAppWidget\(appWidgetId, views\)\n                    \}\n                \}', replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/widget/HotelWidgetProvider.kt", "w") as f:
    f.write(content)
