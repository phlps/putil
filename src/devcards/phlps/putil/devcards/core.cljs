(ns phlps.putil.devcards.core
  (:require
   [sablono.core :as sab :include-macros true]
   [phlps.putil.devcards.parse]
   [phlps.putil.devcards.csv]
   [phlps.putil.devcards.file]
   [phlps.putil.devcards.pouchdb]
   [cljsjs.pouchdb]
   [cljsjs.pouchdb-authentication]
   [cljsjs.pouchdb-find]
   [cljsjs.pouchdb-live-find])
  (:require-macros
   [devcards.core :as dc :refer [defcard deftest]]))

(enable-console-print!)

(defcard first-card
  (sab/html [:div
             [:h1 "This is your first devcard!" ]]))

(defn main []
  ;; conditionally start the app based on whether the #main-app-area
  ;; node is on the page
  #_(.plugin js/PouchDB PouchAuthentication)
  (if-let [node (.getElementById js/document "main-app-area")]
    (.render js/ReactDOM (sab/html [:div "This is working"]) node)))

(main)

;; remember to run lein figwheel and then browse to
;; http://localhost:3449/cards.html
