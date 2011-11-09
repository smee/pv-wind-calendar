(ns eumonis.calendar.views.common
  (:use noir.core
        hiccup.core
        hiccup.page-helpers))

(defpartial layout [& content]
            (html5
              [:head
               [:title "EUMONIS MBS Kalender"]
               (include-css "/css/reset.css")]
              [:body
               [:div#wrapper
                content]]))
