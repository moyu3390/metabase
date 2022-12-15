(ns metabase.shared.formatting.internal.date-builder-test
  (:require
   [clojure.test :refer [are deftest testing]]
   [metabase.shared.formatting.internal.date-builder :as builder]
   [metabase.shared.util.time :as shared.ut]))

;; This tests the underlying string formatter, not the public interface.
(deftest string-formatting-test
  (testing "string formatting"
    (testing "works for keywords and vectors"
      (are [exp t fmt] (= exp ((builder/->formatter fmt) (shared.ut/parse-timestamp t)))
        "2022"                "2022-06-08T09:06:19Z" [:year]
        "202206"              "2022-06-08T09:06:19Z" [:year :month-dd]
        "20226"               "2022-06-08T09:06:19Z" [:year :month-d]
        "2022-06-08"          "2022-06-08T09:06:19Z" [:year ["-"] :month-dd ["-"] :day-of-month-dd]
        "Q2 - 2022"           "2022-06-08T09:06:19Z" [["Q"] :quarter [" - "] :year]

        "6/8/2022"            "2022-06-08T09:06:19Z" [:month-d     ["/"] :day-of-month-d ["/"]  :year]
        "June 8, 2022"        "2022-06-08T09:06:19Z" [:month-full  [" "] :day-of-month-d [", "] :year]
        "Jun 8, 2022"         "2022-06-08T09:06:19Z" [:month-short [" "] :day-of-month-d [", "] :year]
        "Monday June 6, 2022" "2022-06-06T09:06:19Z" [:day-of-week-full [" "] :month-full [" "]
                                                      :day-of-month-d [", "] :year]
        "Mon June 6, 2022"    "2022-06-06T09:06:19Z" [:day-of-week-short [" "] :month-full [" "]
                                                      :day-of-month-d [", "] :year]

        "19:06:19"            "2022-06-08T19:06:19Z" [:hour-24-dd [":"] :minute-dd [":"] :second-dd]
        "07:06:19"            "2022-06-08T19:06:19Z" [:hour-12-dd [":"] :minute-dd [":"] :second-dd]
        "7:06:19"             "2022-06-08T19:06:19Z" [:hour-12-d  [":"] :minute-dd [":"] :second-dd]
        "9:06:19"             "2022-06-08T09:06:19Z" [:hour-24-d  [":"] :minute-dd [":"] :second-dd]
        "7:06 PM"             "2022-06-08T19:06:19Z" [:hour-12-d  [":"] :minute-dd [" "] :am-pm]
        "6m19s"               "2022-06-08T19:06:19Z" [:minute-d ["m"] :second-dd ["s"]]))

    (testing "works for strings in Clojure vectors"
      (are [exp t fmt] (= exp ((builder/->formatter fmt) (shared.ut/parse-timestamp t)))
        "2022"                "2022-06-08T09:06:19Z" ["year"]
        "202206"              "2022-06-08T09:06:19Z" ["year" "month-dd"]
        "20226"               "2022-06-08T09:06:19Z" ["year" "month-d"]
        "2022-06-08"          "2022-06-08T09:06:19Z" ["year" ["-"] "month-dd" ["-"] "day-of-month-dd"]
        "Q2 - 2022"           "2022-06-08T09:06:19Z" [["Q"] "quarter" [" - "] "year"]

        "6/8/2022"            "2022-06-08T09:06:19Z" ["month-d"     ["/"] "day-of-month-d" ["/"]  "year"]
        "June 8, 2022"        "2022-06-08T09:06:19Z" ["month-full"  [" "] "day-of-month-d" [", "] "year"]
        "Jun 8, 2022"         "2022-06-08T09:06:19Z" ["month-short" [" "] "day-of-month-d" [", "] "year"]
        "Monday June 6, 2022" "2022-06-06T09:06:19Z" ["day-of-week-full" [" "] "month-full" [" "]
                                                      "day-of-month-d" [", "] "year"]
        "Mon June 6, 2022"    "2022-06-06T09:06:19Z" ["day-of-week-short" [" "] "month-full" [" "]
                                                      "day-of-month-d" [", "] "year"]

        "19:06:19"            "2022-06-08T19:06:19Z" ["hour-24-dd" [":"] "minute-dd" [":"] "second-dd"]
        "07:06:19"            "2022-06-08T19:06:19Z" ["hour-12-dd" [":"] "minute-dd" [":"] "second-dd"]
        "7:06:19"             "2022-06-08T19:06:19Z" ["hour-12-d"  [":"] "minute-dd" [":"] "second-dd"]
        "9:06:19"             "2022-06-08T09:06:19Z" ["hour-24-d"  [":"] "minute-dd" [":"] "second-dd"]
        "7:06 PM"             "2022-06-08T19:06:19Z" ["hour-12-d"  [":"] "minute-dd" [" "] "am-pm"]
        "6m19s"               "2022-06-08T19:06:19Z" ["minute-d"   ["m"] "second-dd" ["s"]]))

    #?(:cljs
       (testing "works for strings in JS arrays"
         (are [exp t fmt] (= exp ((builder/->formatter fmt) (shared.ut/parse-timestamp t)))
           "2022"                "2022-06-08T09:06:19Z" #js ["year"]
           "202206"              "2022-06-08T09:06:19Z" #js ["year" "month-dd"]
           "20226"               "2022-06-08T09:06:19Z" #js ["year" "month-d"]
           "2022-06-08"          "2022-06-08T09:06:19Z" #js ["year" #js ["-"] "month-dd" #js ["-"] "day-of-month-dd"]
           "Q2 - 2022"           "2022-06-08T09:06:19Z" #js [#js ["Q"] "quarter" #js [" - "] "year"]

           "6/8/2022"            "2022-06-08T09:06:19Z" #js ["month-d"     #js ["/"] "day-of-month-d" #js ["/"]  "year"]
           "June 8, 2022"        "2022-06-08T09:06:19Z" #js ["month-full"  #js [" "] "day-of-month-d" #js [", "] "year"]
           "Jun 8, 2022"         "2022-06-08T09:06:19Z" #js ["month-short" #js [" "] "day-of-month-d" #js [", "] "year"]
           "Monday June 6, 2022" "2022-06-06T09:06:19Z" #js ["day-of-week-full" #js [" "] "month-full" #js [" "]
                                                             "day-of-month-d" #js [", "] "year"]
           "Mon June 6, 2022"    "2022-06-06T09:06:19Z" #js ["day-of-week-short" #js [" "] "month-full" #js [" "]
                                                             "day-of-month-d" #js [", "] "year"]

           "19:06:19"            "2022-06-08T19:06:19Z" #js ["hour-24-dd" #js [":"] "minute-dd" #js [":"] "second-dd"]
           "07:06:19"            "2022-06-08T19:06:19Z" #js ["hour-12-dd" #js [":"] "minute-dd" #js [":"] "second-dd"]
           "7:06:19"             "2022-06-08T19:06:19Z" #js ["hour-12-d"  #js [":"] "minute-dd" #js [":"] "second-dd"]
           "9:06:19"             "2022-06-08T09:06:19Z" #js ["hour-24-d"  #js [":"] "minute-dd" #js [":"] "second-dd"]
           "7:06 PM"             "2022-06-08T19:06:19Z" #js ["hour-12-d"  #js [":"] "minute-dd" #js [" "] "am-pm"]
           "6m19s"               "2022-06-08T19:06:19Z" #js ["minute-d"   #js ["m"] "second-dd" #js ["s"]])))))
