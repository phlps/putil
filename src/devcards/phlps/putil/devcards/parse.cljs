(ns phlps.putil.devcards.parse
  (:require
    [sablono.core :as sab :include-macros true]
    [cljs.reader :as rdr]
    [goog.string :as gstring]
    [phlps.putil.util :as u]
    [phlps.putil.parse :as parse]
    [goog.date :as gdate]
    [devcards.core]
    [cljs.test :as t :refer [report] :include-macros true]
    #_[phsutil.devcards.data.fake :as fake] ; addresses-au
    #_[phsutil.devcards.data.subjects :as subjects]
    #_[phsutil.devcards.data.modules :as modules])
  (:require-macros
    [devcards.core :as dc :refer [defcard defcard-doc deftest]]
    [cljs.test :refer [is testing async]]))



(def demodate (gdate/DateTime. (.getFullYear (gdate/DateTime.)) 11 25 1 2 3))

(defcard date-time-formatting-and-parsing
            (let [now (gdate/DateTime.)]
              (sab/html
                [:div
                [:table
                 [:thead [:tr [:th "specification.."] [:th " "] [:th "sample"]]]
                 [:tbody
                  (for [f u/date-formats]
                    ^{:key f}
                    [:tr [:td f] [:td " : "] [:td (u/datestr demodate f)]])]]
                [:table
                 [:thead [:tr [:th "std. spec."] [:th " "] [:th "sample"]]]
                 [:tbody
                  (for [f (range 12)]
                    ^{:key f}
                    [:tr [:td f] [:td " : "] [:td (u/datestr now f)]])]]])))




(deftest test-numbers
         (testing "test parsing for integers and numbers"
           (is (= (parse/integer "94.85") nil))
           (is (= (parse/number "94.85") 94.85))
           (is (u/NaN? (parse/integer "52.2km")))
           (is (u/NaN? (parse/number "52.2km")))))


;[[:subsection {:title "u/randUUID : a random UUID"}]]

(deftest test-UUID
         (testing "a few tests for UUIDs"
           (is (= (count (str (u/rand-uuid))) 36))
           (is (uuid? (u/rand-uuid)))))


(deftest test-date-parsing
         (testing
            "If it's obviously an ISO date, it's easy to get it right.
             Hyphens are required for an ISO date, and an ISO date is the
             only common format with the year at its beginning."
           ;
           (is (= (str (parse/date "1986-10-14")) "19861014T000000"))
           (is (= (str (parse/date "1986-7-12")) "19860712T000000"))
           (is (not (u/date? (parse/date "1986/7/12"))))

            ) (testing
                "The most common format has the US order MM/dd/yy.
                  Remember hyphens prove an ISO date and so the year is first."
                ;
           (is (not (u/date? (parse/date "1-06-1988"))))
           (is (= (str (parse/date "10/12/1943")) "19431012T000000"))
           (is (= (str (parse/date "12/25/98")) "19981225T000000"))
           (is (= (str (parse/date "10/11/12")) "20121011T000000"))
           (is (= (str (parse/date "10-11-12")) "101112T000000"))
           ;
           ; Not 32 days in December, not 13 months in 1998
           ;
           (is (not (u/date? (parse/date "12/32/1998"))))
           (is (not (u/date? (parse/date "13/25/1998"))))
           (is (= (str (parse/date "12/25/998")) "9981225T000000"))
           ;
           "In the following examples a format parameter is provided."
           ; If the date is not like the format, it won't parse.
           ;    Slashes won't match hyphens.
           ;
           (is (not (u/date? (parse/date "25/12/1909" "MM/yyyy"))))
           (is (not (u/date? (parse/date "10-12-1934" "dd/MM/yyyy"))))
           ;
           ; Numerically impossible things won't parse. There are not 25 months
           ; in a year.
           ;
           (is (not (u/date? (parse/date "25/12/1957" "MM/dd/yy"))))

           ; Long ago dates

           (is (= (str (parse/date "10/12/64" "MM/dd/yyyy")) "641012T000000"))
           ;
           ; Plenty of ways to successfully parse a date.
           ;
           (is (= (str (parse/date "25/12/1909" "dd/MM/yyyy")) "19091225T000000"))
           (is (= (str (parse/date "25-12-1909" "dd-MM-yyyy")) "19091225T000000"))
           (is (= (str (parse/date "10/12/1934" "d/M/yy")) "19341210T000000"))
           (is (= (str (parse/date "10-12-1934" "d-M-yy")) "19341210T000000"))
           (is (= (str (parse/date "10/12/1934" "MM/dd/yyyy")) "19341012T000000"))
           (is (= (str (parse/date "10/12/1935" "MM/dd/yyyy")) "19351012T000000"))
           (is (= (str (parse/date "10/12/1935" "M/d/yy")) "19351012T000000"))
           (is (= (str (parse/date "10-12-1935" "M-d-yy")) "19351012T000000"))
           ;
           ; You can shoot yourself in the foot by providing a silly
           ;   format that accepts nonsense. VERY unwise to flout common
           ;   communication and language conventions.
           ;
           (is (= (str (parse/date "10/2012/13" "M/yy/d")) "20121013T000000"))
           ;
           ; 2-digit years are within a century of now, unless you  specify yyyy.
           ;
           (is (= (str (parse/date "10/12/42" "d/M/yy")) "19421210T000000"))
           (is (= (str (parse/date "10/12/75" "MM/dd/yy")) "19751012T000000"))
           (is (= (str (parse/date "10/12/35" "M/d/yy")) "20351012T000000"))
           (is (= (str (parse/date "10/12/13" "M/d/yy")) "20131012T000000"))
           ;
           ;
           ; You can parse the time too
           ;
           (is (= (str (parse/date "1/06/1988 0:00:00" "d/M/yy h:m:s"))
                  "19880601T000000"))
           (is (= (str (parse/date "1/06/1988 21:30:40" "d/M/yy hh:mm:ss"))
                  "19880601T213040"))
           (is (= (str (parse/date "1/06/1988 08:30:40" "d/M/yy HH:mm:ss"))
                  "19880601T083040"))


           )
         #_(testing "parse data which came from CSV"
           (is (= (parse/row-stats ["12" "" "12.9" "12.9km" "12/25/16"] nil)
                  [{:integer 1} {:blank 1} {:number 1} {:string 1} {"M/d/yy" 1}]))
           (is (= (parse/stats fake/addresses-au 500)
                  [{:name "phone", :string 499}
                   {:name "mobile", :string 499}
                   {:name "email", :string 499}
                   {:name "address", :string 499}
                   {:name "city", :string 499}
                   {:name "postcode", :integer 499}
                   {:name "state", :string 499}]
                  ))
           (is (= (parse/stats subjects/subjects 500)
                  [{:name "SUBJ_ID", :integer 499}
                   {:name "SUBJ_NAME", :string 499}
                   {:name "SUBJ_TYPE", :string 499}
                   {:name "SUBJ_START_DATE", "M/d/yy" 467, "d/M/yy" 32}
                   {:name "SUBJ_CEASE_DATE", "d/M/yy" 469, :blank 30}
                   {:name "SUBJ_CLASS", :string 499}
                   {:name "SUBJ_COMMENT", :blank 497, :string 2}
                   {:name "DISCIPLINE_GROUP", :integer 378, :blank 121}
                   {:name "FIELD_OF_STUDY", :integer 382, :blank 117}
                   {:name "PREREQ_SUBJ_ID", :blank 499}
                   {:name "NAME", :string 499}
                   {:name "CERT_MODULE_GROUP", :blank 450, :string 49}
                   {:name "KLA_CODE_TYPE", :blank 468, :string 31}
                   {:name "KLA", :blank 468, :string 31}
                   {:name "KLA_DETYA", :string 499}
                   {:name "EXTENSION_SUBJECT", :string 499}]
                  ))
           (is (= (parse/stats modules/modules 1000)
                  [{:name "MODULE_ID", :integer 999}
                   {:name "MODULE_CODE", :string 999}
                   {:name "MODULE_NAME", :string 999}
                   {:name "COURSE_ID", :integer 999}
                   {:name "NOMINAL_HOURS", :integer 900, :blank 99}
                   {:name "DISCIPLINE_GROUP", :integer 991, :blank 8}
                   {:name "SOURCE", :string 993, :blank 6}
                   {:name "TYPE", :string 999}
                   {:name "NATIONAL_MODULE_ID", :blank 999}
                   {:name "FIELD_OF_EDUCATION", :blank 977, :integer 22}]
                  ))
           ))


"What follows is an example of how to test with com.cemerick/clojurescript.test"

(comment
  (dc/deftest test-str-to-date
              (is (= (str (parse/date "M/d/yy" "10/12/13")) "0013-10-12"))))
