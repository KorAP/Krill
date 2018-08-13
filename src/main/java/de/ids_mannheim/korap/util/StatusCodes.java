package de.ids_mannheim.korap.util;

public class StatusCodes {
    // 600 - 699 - Krill server error codes
    public static final int UNABLE_TO_READ_INDEX = 600;
    public static final int UNABLE_TO_FIND_INDEX = 601;
    public static final int UNABLE_TO_ADD_DOC_TO_INDEX = 602;
    public static final int UNABLE_TP_COMMIT_STAGED_DATA_TO_INDEX = 603;
    public static final int UNABLE_TO_CONNECT_TO_DB = 604;
    public static final int MISSING_KRILL_PROPERTIES = 605;
    public static final int MISSING_REQUEST_PARAMETER = 610;
    public static final int ARBITRARY_DESERIALIZATION_ERROR = 613;
    public static final int UNABLE_TO_GENERATE_JSON = 620;
    public static final int UNABLE_TO_PARSE_JSON = 621;
    public static final int DOCUMENT_NOT_FOUND = 630;
    public static final int UNABLE_TO_EXTEND_CONTEXT = 651;
    public static final int SERVER_IS_RUNNING = 680;
    public static final int DOC_ADDED = 681;
    public static final int RESPONSE_TIME_EXCEEDED = 682;
    public static final int STAGED_DATA_COMMITTED = 683;

    // 700 - 799 - KoralQuery Deserialization errors
    public static final int NO_QUERY_GIVEN = 700;
    public static final int MISSING_TYPE = 701;
    public static final int INVALID_BOUNDARY = 702;
    public static final int MISSING_OPERATION = 703;
    public static final int MISSING_OPERAND_LIST = 704;
    public static final int NUMBER_OF_OPERAND_NOT_ACCEPTABLE = 705;
    public static final int UNKNOWN_FRAME_TYPE = 706;
    public static final int INVALID_DISTANCE_CONSTRAINTS = 707;
    public static final int MISSING_DISTANCE = 708;
    public static final int VALID_CLASS_NUMBER_EXCEEDED = 709;
    public static final int MISSING_CLASS_ATTRIBUTE = 710;
    public static final int UNKNOWN_GROUP_OPERATION = 711;
    public static final int UNKNOWN_REFERENCE_OPERATION = 712;
    public static final int UNSUPPORTED_QUERY = 713;
    public static final int INVALID_SPAN_REFERENCE_PARAMETER = 714;
    public static final int UNSUPPORTED_ATTRIBUTE_TYPE = 715;
    public static final int UNKNOWN_RELATION = 716;
    public static final int MISSING_RELATION_NODE = 717;
    public static final int MISSING_RELATION_TERM = 718;
    public static final int INVALID_QUERY = 719;
    public static final int MISSING_VC_REFERENCE = 720;
    
    public static final int INVALID_MATCH_ID = 730;
    public static final int MISSING_KEY = 740;
    public static final int UNKNOWN_MATCH_RELATION = 741;
    public static final int MISSING_TERM_RELATION = 743;
    public static final int UNSUPPORTED_OPERAND = 744;
    public static final int UNSUPPORTED_TOKEN_TYPE = 745;
    public static final int UNSUPPORTED_TERM_TYPE = 746;
    public static final int NULL_ATTRIBUTE = 747;
    public static final int UNKNOWN_FLAG = 748;
    public static final int NON_WELL_FORMED_NOTIFICATION = 750;
    public static final int UNSUPPORTED_OPERATION = 760;
    public static final int UNSUPPORTED_OPERATOR = 761;
    public static final int QUERY_MATCH_EVERYWHERE = 780;
    public static final int IGNORE_OPTIONALITY = 781;
    public static final int IGNORE_EXCLUSIVITY = 782;
    public static final int QUERY_CANNOT_MATCH_ANYWHERE = 783;
    public static final int UNKNOWN_QUERY_SERIALIZATION_MESSAGE = 799;

    // 800 - 899 - Virtual Collection Messages
    public static final int MISSING_COLLECTION = 800;
    public static final int UNSUPPORTED_MATCH_TYPE = 802;
    public static final int UNKNOWN_VALUE_TYPE = 804;
    public static final int INVALID_VALUE = 805;
    public static final int INVALID_DATE_STRING = 806;
    public static final int INVALID_REGEX = 807;
    public static final int UNKNOWN_REWRITE_OPERATION = 814;
    public static final int MISSING_SOURCE = 815;
    public static final int MISSING_ID = 816;
    public static final int MISSING_VALUE = 820;
    public static final int EMPTY_FILTER = 830;
    public static final int UNWRAPPABLE_FILTER = 831;
    public static final int INVALID_FILTER_OPERATION = 832;
    public static final int UNSUPPORTED_TYPE = 843;
    public static final int COLLECTIONS_UNSUPPORTED = 850;
    public static final int CACHING_COLLECTION_FAILED = 851;

    // 900 - 999 - Corpus Data errors 
    public static final int INVALID_OFFSET = 952;
    public static final int INCOMPLETE_OFFSET = 953;
    public static final int INVALID_FOUNDRY = 970;

}
