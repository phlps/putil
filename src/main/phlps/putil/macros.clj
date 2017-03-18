(ns phlps.putil.macros)

(defmacro <? [ch]
  `(phlps.putil.util/throw-err (cljs.core.async/<! ~ch)))
