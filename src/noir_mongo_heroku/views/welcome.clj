(ns noir-mongo-heroku.views.welcome
  (:require [noir-mongo-heroku.views.common :as common]
            [noir.content.pages :as pages])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers)
  (:import [net.fortuna.ical4j.model Calendar Property Date]
           [net.fortuna.ical4j.model.property ProdId Version CalScale]
           net.fortuna.ical4j.model.component.VEvent
           net.fortuna.ical4j.model.parameter.Value
           net.fortuna.ical4j.util.UidGenerator))

(defpage "/welcome" []
         (common/layout
           [:p "Welcome to noir-mongo-heroku"]))

(defpage "/cal/:plz" {plz :plz}
  (let [cal (Calendar.)
        _ (doto (.getProperties cal)
            (.add (ProdId. "-//Steffen Dienst//test 1.0//EN"))
            (.add Version/VERSION_2_0)
            (.add CalScale/GREGORIAN))
        now (System/currentTimeMillis)
        event (doto (VEvent. (Date. now) (str "kleiner Test genau heute für PLZ " plz))
                ;(.. getProperties (getProperty Property/DTSTART) getParameters (add Value/DATE))
                (.. getProperties (add (.generateUid (UidGenerator. "1")))))
        output (net.fortuna.ical4j.data.CalendarOutputter.)
        buf (java.io.StringWriter.)]
    (.. cal getComponents (add event))
    (.output output cal buf)
    (str buf)
    ))