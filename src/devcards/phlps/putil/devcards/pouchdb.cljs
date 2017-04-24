
(ns phlps.putil.devcards.pouchdb
  (:require
    [devcards.core] ;:as dc :refer [defcard defcard-doc deftest]]
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
    [devcards.core :as dc :refer [defcard-doc deftest]]
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
 (testing "testing pouchdb: open put post info get destroy "
   (async done
     (go
     (let
         [out (chan)
          db (pouch/open "test-silly-db")
          a1 {:Xid "a1", :long "111"}
          a2 {:wide 222}
          b1 (pouch/post db a1 out)
          b1 (<! out)
          z0 (prn "b1 " )
          b2 (pouch/post db a2 out)
          b2 (<! out)
          z0 (prn "b2 " b2)
          c1 (get b1 "id")
          z0 (prn "c1 " c1)
          c2 (get b2 "id")
          z0 (prn "c2 " c2)
          d1 (get b1 "rev")
          d2 (get b2 "rev")
          inf (pouch/info db out)
          inf (<! out)
          z0 (prn "info " inf)
          g1 (pouch/get db c1 out)
          g1 (<! out)
          g2 (pouch/get db c2 out)
          g2 (<! out)
          z0 (prn "g1 " g1)
          z0 (prn "g2 " g2)
          ]
         (do
           (is (= c1 (get g1 "_id")) (str "'_id' of first document " c1))
           (is (= c2 (get g2 "_id")) (str "'_id' of second document " c2))
           (is (= d1 (get g1 "_rev")) (str "'_rev' of first document " d1))
           (is (= d2 (get g2 "_rev")) (str "'_rev' of second document " d2))
           (is (= 222 (get g2 "wide")) "'wide' of second document 222")
           (is (= (get inf "doc_count") 2) "doc count of database")
           (is (= (get inf "db_name") "test-silly-db") " name of database")
           )
         (pouch/destroy db out)
       (done))))))

(comment
  (require '[phlps.putil.pouchdb :as pouch]
           '[cljs.core.async :refer [>! <! chan put! close! timeout]]
           )
  (require '[cljs.core.async.macros :as mac :refer [go go-loop alt!]])
  (def a2 {:wide 222})
  (def db (pouch/open "test-silly-db"))
  (cljs.core.async.macros/go (prn "b2" (cljs.core.async/<! (pouch/post db a2))))
  )


