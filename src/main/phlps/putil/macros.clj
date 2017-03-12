(ns phlps.putil.macros)

(defmacro <? [ch]
  `(phs.util/throw-err (cljs.core.async/<! ~ch)))
