(ns eumonis.calendar.icalendar
  (:import [net.fortuna.ical4j.model Calendar Property Date DateTime]
           [net.fortuna.ical4j.model.property ProdId Version CalScale]
           net.fortuna.ical4j.model.component.VEvent
           net.fortuna.ical4j.model.parameter.Value
           net.fortuna.ical4j.util.UidGenerator))

(def ^:private uid-gen (UidGenerator. "1"))

(defn create-allday-event [date text]
  (doto (VEvent. (Date. date) text)
    (.. getProperties (add (.generateUid uid-gen)))))

(defn create-event [start end text]
  (doto (VEvent. (DateTime. start) (DateTime. end) text)
    (.. getProperties (add (.generateUid uid-gen)))))


(defn create-icalendar [events]
  (let [cal (Calendar.)
        now (System/currentTimeMillis)
        output (net.fortuna.ical4j.data.CalendarOutputter.)
        buf (java.io.StringWriter.)]
    (doto (.getProperties cal)
            (.add (ProdId. "-//Steffen Dienst//EUMONIS Test 1.0//EN"))
            (.add Version/VERSION_2_0)
            (.add CalScale/GREGORIAN))
    (.. cal getComponents (addAll events))
    (.output output cal buf)
    (str buf)))