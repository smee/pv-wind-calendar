(defproject eumonis-calendar "0.1.0-SNAPSHOT"
            :description "FIXME: write this!"
            :dev-dependencies [[lein-ring "0.4.6"]] 
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [noir "1.2.1"]
                           [org.mnode.ical4j/ical4j "1.0.2"]
                           [org.clojars.sritchie09/enlive "1.2.0-alpha1"]]
            :main eumonis.calendar.server
            :ring {:handler eumonis.calendar.server/handler})

