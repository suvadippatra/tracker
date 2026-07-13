import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    manifest = f.read()

receivers = """
        <receiver android:name=".widget.HotelWidgetProvider" android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/widget_hotel_info" />
        </receiver>

        <receiver android:name=".widget.ExpensesWidgetProvider" android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/widget_expenses_info" />
        </receiver>

        <receiver android:name=".widget.OverallWidgetProvider" android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/widget_overall_info" />
        </receiver>
"""

if "<receiver android:name=\".widget.HotelWidgetProvider\"" not in manifest:
    # insert before </application>
    manifest = manifest.replace("</application>", receivers + "\n    </application>")
    with open("app/src/main/AndroidManifest.xml", "w") as f:
        f.write(manifest)
    print("Added widget receivers to manifest")
else:
    print("Widget receivers already in manifest")
