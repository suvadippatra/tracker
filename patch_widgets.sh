#!/bin/bash

# widget_expenses_compact.xml
sed -i 's/16sp/12sp/g' app/src/main/res/layout/widget_expenses_compact.xml
sed -i 's/18dp/16dp/g' app/src/main/res/layout/widget_expenses_compact.xml
sed -i 's/9sp/8sp/g' app/src/main/res/layout/widget_expenses_compact.xml
sed -i 's/8sp/7sp/g' app/src/main/res/layout/widget_expenses_compact.xml

# widget_hotel_compact.xml
sed -i 's/11sp/10sp/g' app/src/main/res/layout/widget_hotel_compact.xml
sed -i 's/18dp/16dp/g' app/src/main/res/layout/widget_hotel_compact.xml
sed -i 's/8sp/7sp/g' app/src/main/res/layout/widget_hotel_compact.xml

# widget_spend_compact.xml
sed -i 's/18dp/16dp/g' app/src/main/res/layout/widget_spend_compact.xml
sed -i 's/9sp/8sp/g' app/src/main/res/layout/widget_spend_compact.xml
sed -i 's/8sp/7sp/g' app/src/main/res/layout/widget_spend_compact.xml

# widget_overall_compact.xml
sed -i 's/11sp/10sp/g' app/src/main/res/layout/widget_overall_compact.xml
sed -i 's/18dp/16dp/g' app/src/main/res/layout/widget_overall_compact.xml
sed -i 's/9sp/8sp/g' app/src/main/res/layout/widget_overall_compact.xml
sed -i 's/8sp/7sp/g' app/src/main/res/layout/widget_overall_compact.xml

echo "Widgets patched."
