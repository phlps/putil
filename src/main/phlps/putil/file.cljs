(ns phlps.putil.file
  (:require
    [clojure.string :as str]
    [cljs.core.async
     :as a
     :refer [>! <! chan buffer close!
             alts! timeout]])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn- log [s]
  (.log js/console s))

(defn- browsercapable [what]
  (let [names (str/split what #",")
        can #(aget js/window %)
        caps (filter can names)]
    (= (count caps) (count names))))

(defn browser-can-read-files []
  (browsercapable "File,FileReader,FileList,Blob"))

(defn- on-file-select [on-files]
  "Return a handler for the `on-change` event of an <input type='file'>."
  (let [evt-handler (fn [evt]
                      (on-files (-> evt .-target .-files)))]
    evt-handler))

(defn select [on-files multiple?]
  "A reagent component to select file/s within the browser."
  (let [att0 {:type "file" :on-change (on-file-select on-files)}
        attrs (if multiple? (assoc att0 :multiple true) att0)]
    (if (browser-can-read-files)
       [:div [:input attrs]]
       [:div [:h2 "This browser can't import a file."]])))

(defn file-segments
  ([file]
   (file-segments 100000 file))
  ([segment-size file]
   (file-segments segment-size 0 file))
  ([segment-size max-segments file]
   "The file will be broken up into large strings called 'segments'
   and the segments will be pushed into a channel we will call `out`."
   (let [out (chan)
         size (.-size file)
         filename (.-name file)
         x (println (str "Processing File: "
                       filename " Size: " size))
         count1 (int (/ size segment-size))
         remainder (mod size segment-size)
         sizeof-last-segment (if (= remainder 0) segment-size remainder)
         segment-count (if (= remainder 0) count1 (+ count1 1))
         segments-to-do (if (and (not= max-segments 0) (< max-segments segment-count)) max-segments segment-count)
         sizeof-first-segment (if (= 1 segment-count) sizeof-last-segment segment-size)
         ;xx (println segments-to-do)
         segments-done (chan)
         rdr (js/FileReader.)
         onabort (fn [evt]
                   (println "onabort event")
                   (go (>! out (js/Error. "File cancelled at your request."))
                       (close! out)))
         onerror (fn [evt]
                   (let [e (-> evt .-target .-error .-name)
                         er (str "Error while reading file: " e)
                         err (js/Error. er)]
                     (println "onerror event")
                     (go (>! out err)
                         (close! out))))
         onload (fn [evt]
                  "send the segment on the out channel,
                   and then call another readAsText"
                  (go
                    (>! out (-> evt .-target .-result))
                    (let [done (<! segments-done)
                          next (+ 1 done)
                          start (* segment-size done)
                          size (if (= segments-to-do next) sizeof-last-segment segment-size)]
                      ;(println (str "Exported segment " done " ..."))
                      (if (<= next segments-to-do)
                        (do (.readAsText rdr (.slice file start (+ start size)))
                            (>! segments-done next))
                        (do (println "FINISHED file " filename)
                            (close! segments-done)
                            (close! out))))))]
     (aset rdr "onabort" onabort)
     (aset rdr "onerror" onerror)
     (aset rdr "onload" onload)
     (if (> segments-to-do 0)
       (go "output-the first segment"
           (.readAsText rdr (.slice file 0 sizeof-first-segment))
           (>! segments-done 1))
       (go (>! out (js/Error. "File was empty. No segments."))
           (close! segments-done)
           (close! out)))
     out)))

(defn text-lines [in]
  "The in channel provides a series of text-segments which
  were willy-nilly chopped up from a larger piece of text.
  The out channel provides a series of vectors, each vector containing
  strings. Each string is a line of text with the \n chars removed.
  Correct ordering is maintained."
  (let [out (chan)]
    (go-loop
      [rem ""]
      (if-let [segment (<! in)]
        (let [whole (str rem segment)
              ;_ (println (last whole))
              ends-nl (= "\n" (last whole))
              lines (str/split-lines whole)]
          (if ends-nl
            (do #_(println "1")
              (>! out lines)
              #_(println "1a")
              (recur ""))
            (do #_(println "2")
              (>! out (subvec lines 0 (dec (count lines))))
              (recur (last lines)))))
        (do (if (> (count rem) 0)
              (>! out rem))
            #_(println "3")
            (close! out))))
    out))
