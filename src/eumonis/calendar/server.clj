(ns eumonis.calendar.server
  (:require [noir.server :as server]
            ;; view namespaces need to be required explicitely for tomcat
            [eumonis.calendar.views 
             common
             welcome])
  (:gen-class))

;; redundant to requiring the view namespace in the ns form above
(server/load-views "src/eumonis/calendar/views/")


(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode mode
                        :ns 'eumonis.calendar
                        ;:base-url "eumonis-calendar/"
                        })))

(def handler (server/gen-handler {:mode :dev
                                  :ns 'eumonis.calendar
                                  :base-url "eumonis-calendar"}))

