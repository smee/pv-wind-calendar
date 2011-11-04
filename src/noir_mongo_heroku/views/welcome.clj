(ns noir-mongo-heroku.views.welcome
  (:require [noir-mongo-heroku.views.common :as common]
            [noir.content.pages :as pages]
            [noir-mongo-heroku.scrape.agrarwetter :as wetter])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers)
  (:import [net.fortuna.ical4j.model Calendar Property Date]
           [net.fortuna.ical4j.model.property ProdId Version CalScale]
           net.fortuna.ical4j.model.component.VEvent
           net.fortuna.ical4j.model.parameter.Value
           net.fortuna.ical4j.util.UidGenerator))

(def ^:private df (java.text.SimpleDateFormat. "dd.MM.yyyy"))

(defpage "/welcome" []
         (common/layout
           [:p "Welcome to noir-mongo-heroku"]))

(defn- create-day-event [date s]
  (doto (VEvent. (Date. date) (str "Sonneneinstrahlung gesamt bei " s))
    (.. getProperties (add (.generateUid (UidGenerator. "1"))))))

(defpage "/cal/:plz" {plz :plz}
  (let [cal (Calendar.)
        _ (doto (.getProperties cal)
            (.add (ProdId. "-//Steffen Dienst//test 1.0//EN"))
            (.add Version/VERSION_2_0)
            (.add CalScale/GREGORIAN))
        now (System/currentTimeMillis)
        daily (wetter/get-occlusion-day (wetter/retrieve plz))
        events (map (fn [[date s]] (create-day-event (.parse df date) s)) daily) 
        output (net.fortuna.ical4j.data.CalendarOutputter.)
        buf (java.io.StringWriter.)]
    (.. cal getComponents (addAll events))
    (.output output cal buf)
    (str buf)
    ))