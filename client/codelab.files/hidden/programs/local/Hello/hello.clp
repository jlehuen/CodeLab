;;
;; Program hello.clp
;;

(defrule only-rule
	=>
	(printout t "Hello World from " (version) crlf))
