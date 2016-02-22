# Error Codes in Krill

## 600 - 699 - Krill server error codes
```
600: "Unable to read index"
601: "Unable to find index"
602: "Unable to add document to index"
603: "Unable to commit staged data to index"
604: "Unable to connect to database"
610: "Missing request parameters"
620: "Unable to generate JSON"
621: "Unable to parse JSON"
651: "Unable to extend context"
680: "Server is up and running!"
681: "Document was added successfully", document id
682: "Response time exceeded"
683: "Staged data committed"
```

## 700 - 799 - KoralQuery Deserialization errors
```
700: "No Query given"
701: "JSON-LD group has no @type attribute"
702: "Boundary definition is invalid"
703: "Group expects operation"
704: "Operation needs operand list"
705: "Number of operands is not acceptable"
706: "Frame type is unknown"
707: "Distance Constraints have to be defined as arrays"
708: "No valid distances defined"
709: "Valid class numbers exceeded"
710: "Class attribute missing"
711: "Unknown group operation"
712: "Unknown reference operation"
713: "Query type is not supported"
714: "Span references expect a start position and a length parameter"
715: "Attribute type is not supported"
716: "Unknown relation"
717: "Missing relation node"
718: "Missing relation term"
730: "Invalid match identifier"
740: "Key definition is missing in term or span"
741: "Match relation unknown"
742: "Term group needs operand list"
743: "Term group expects a relation"
744: "Operand not supported in term group"
745: "Token type is not supported"
746: "Term type is not supported - treated as a string"
747: "Attribute is null"
748: "Flag is unknown"
750: "Passed notifications are not well formed"
760: "Exclusion is currently not supported in position operations"
761: "Class reference operators are currently not supported"
762: "Span references are currently not supported" (Not in use)
763: "Excluding distance constraints are currently not supported"
764: "Class reference checks are currently not supported - results may not be correct"
765: "Relations are currently not supported"
766: "Peripheral references are currently not supported"
767: "Case insensitivity is currently not supported for this layer"
768: "Attributes are currently not supported - results may not be correct"
769: "Overlap variant currently interpreted as overlap"
770: "Arity attributes are currently not supported - results may not be correct"
771: "Arbitrary elements with attributes are currently not supported"
780: "This query matches everywhere"
781: "Optionality of query is ignored"
782: "Exclusivity of query is ignored"
799: Unknown query serialization message (Arbitrary string)
```

## 800 - 899 - Virtual Collection Messages
```
802: "Match type is not supported by value type"
804: "Unknown value type"
805: "Value is invalid"
806: "Value is not a valid date string"
807: "Value is not a valid regular expression"
810: "Unknown document group operation" (like 711)
811: "Document group expects operation" (like 703)
812: "Operand not supported in document group" (like 744)
813: "Collection type is not supported" (like 713)
814: "Unknown rewrite operation"
815: "Rewrite expects source"
820: "Dates require value fields"
830: "Filter was empty"
831: "Filter is not wrappable"
832: "Filter operation is invalid"
841: "Match relation unknown for type" (like 741)
842: "Document group needs operand list"
843: "Document type is not supported"
850: "Collections are deprecated in favour of a single collection"
851: "Legacy filters need @value fields"
899: "Collections are not supported anymore in favour of a single collection"
```

## 900 - 999 - Corpus Data errors
```
952: "Given offset information is not numeric"
953: "Given offset information is incomplete"
970: "Invalid foundry requested"
```
