(ns datestuff
  (:use clojure.test)
  (:require [eumonis.calendar.views.welcome :as w]))

(deftest overlapping
  (is (= true (w/overlaps? [1 3] [2 4])))
  (is (= true (w/overlaps? [2 4] [1 3])))
  (is (= true (w/overlaps? [1 4] [2 3])))
  (is (= true (w/overlaps? [2 3] [1 4]))))