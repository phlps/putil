
(ns phlps.putil.devcards.pouchdb
  (:require
    [devcards.core :as dc :refer [defcard defcard-doc deftest]]
    [cljs.test :as t :refer [is testing async report] #_:include-macros #_true]
    [phlps.putil.util :as u]
    [phlps.putil.parse :as parse]
    [phlps.putil.pouchdb :as pouch]
    [phlps.putil.chan :as ch]
    #_[sablono.core :as sab :include-macros true]
    #_[clojure.spec :as spec]
    #_[clojure.spec.test :as stest]
    [cljs.core.async :refer [>! <! chan put! close! timeout]]
    )
  (:require-macros
    [phlps.putil.macros :refer [<?]]
    #_[devcards.core :as dc :refer [defcard defcard-doc deftest]]
    #_[cljs.test :as t :refer [is testing async]]
    [cljs.core.async.macros :refer [go go-loop alt!]]))

#_(set! devcards.core/test-timeout 999)
#_(comment
  (require '[phsutil.pouchdb :as pouch])
  (go (let [db (pouch/open "test-silly-db")
            r (pouch/destroy db)
            r (<! r)
            r (prn r)]
        123)))
(deftest test-pouch-simple
 (testing "testing pouchdb: open put post info get destry"
   (async done (go
     (let
       #_[db (pouch/open "test-silly-db")]
       [db (pouch/open "test-silly-db")
          a1 {:_id "a1" :long "111"}
          a2 {:wide 222}
          b1 (<! (pouch/put db a1))
          b2 (<! (pouch/post db a2))
          c1 (get b1 "id")
          c2 (get b2 "id")
          inf (<! (pouch/info db))
          g1 (<! (pouch/get db c1))
          g2 (<! (pouch/get db c2))
          ]
         (do
           (is (= "a1" c1) "'_id' of first document \"a1\"")
           (is (= "a1" (get g1 "_id")) "'_id' of first document \"a1\"")
           (is (= c2 (get g2 "_id")) (str "'_id' of second document " c2))
           (is (= 222 (get g2 "wide")) "'wide' of second document 222")
           (is (= (get inf "doc_count") 2) "doc count of database")
           (is (= (get inf "db_name") "test-silly-db") " name of database")
           )
         (pouch/destroy db)
       (done))))))





