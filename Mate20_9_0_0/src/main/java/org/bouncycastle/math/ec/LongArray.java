package org.bouncycastle.math.ec;

import java.math.BigInteger;
import org.bouncycastle.asn1.cmc.BodyPartID;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.eac.CertificateBody;
import org.bouncycastle.util.Arrays;

class LongArray implements Cloneable {
    private static final short[] INTERLEAVE2_TABLE = new short[]{(short) 0, (short) 1, (short) 4, (short) 5, (short) 16, (short) 17, (short) 20, (short) 21, (short) 64, (short) 65, (short) 68, (short) 69, (short) 80, (short) 81, (short) 84, (short) 85, (short) 256, (short) 257, (short) 260, (short) 261, (short) 272, (short) 273, (short) 276, (short) 277, (short) 320, (short) 321, (short) 324, (short) 325, (short) 336, (short) 337, (short) 340, (short) 341, (short) 1024, (short) 1025, (short) 1028, (short) 1029, (short) 1040, (short) 1041, (short) 1044, (short) 1045, (short) 1088, (short) 1089, (short) 1092, (short) 1093, (short) 1104, (short) 1105, (short) 1108, (short) 1109, (short) 1280, (short) 1281, (short) 1284, (short) 1285, (short) 1296, (short) 1297, (short) 1300, (short) 1301, (short) 1344, (short) 1345, (short) 1348, (short) 1349, (short) 1360, (short) 1361, (short) 1364, (short) 1365, (short) 4096, (short) 4097, (short) 4100, (short) 4101, (short) 4112, (short) 4113, (short) 4116, (short) 4117, (short) 4160, (short) 4161, (short) 4164, (short) 4165, (short) 4176, (short) 4177, (short) 4180, (short) 4181, (short) 4352, (short) 4353, (short) 4356, (short) 4357, (short) 4368, (short) 4369, (short) 4372, (short) 4373, (short) 4416, (short) 4417, (short) 4420, (short) 4421, (short) 4432, (short) 4433, (short) 4436, (short) 4437, (short) 5120, (short) 5121, (short) 5124, (short) 5125, (short) 5136, (short) 5137, (short) 5140, (short) 5141, (short) 5184, (short) 5185, (short) 5188, (short) 5189, (short) 5200, (short) 5201, (short) 5204, (short) 5205, (short) 5376, (short) 5377, (short) 5380, (short) 5381, (short) 5392, (short) 5393, (short) 5396, (short) 5397, (short) 5440, (short) 5441, (short) 5444, (short) 5445, (short) 5456, (short) 5457, (short) 5460, (short) 5461, (short) 16384, (short) 16385, (short) 16388, (short) 16389, (short) 16400, (short) 16401, (short) 16404, (short) 16405, (short) 16448, (short) 16449, (short) 16452, (short) 16453, (short) 16464, (short) 16465, (short) 16468, (short) 16469, (short) 16640, (short) 16641, (short) 16644, (short) 16645, (short) 16656, (short) 16657, (short) 16660, (short) 16661, (short) 16704, (short) 16705, (short) 16708, (short) 16709, (short) 16720, (short) 16721, (short) 16724, (short) 16725, (short) 17408, (short) 17409, (short) 17412, (short) 17413, (short) 17424, (short) 17425, (short) 17428, (short) 17429, (short) 17472, (short) 17473, (short) 17476, (short) 17477, (short) 17488, (short) 17489, (short) 17492, (short) 17493, (short) 17664, (short) 17665, (short) 17668, (short) 17669, (short) 17680, (short) 17681, (short) 17684, (short) 17685, (short) 17728, (short) 17729, (short) 17732, (short) 17733, (short) 17744, (short) 17745, (short) 17748, (short) 17749, (short) 20480, (short) 20481, (short) 20484, (short) 20485, (short) 20496, (short) 20497, (short) 20500, (short) 20501, (short) 20544, (short) 20545, (short) 20548, (short) 20549, (short) 20560, (short) 20561, (short) 20564, (short) 20565, (short) 20736, (short) 20737, (short) 20740, (short) 20741, (short) 20752, (short) 20753, (short) 20756, (short) 20757, (short) 20800, (short) 20801, (short) 20804, (short) 20805, (short) 20816, (short) 20817, (short) 20820, (short) 20821, (short) 21504, (short) 21505, (short) 21508, (short) 21509, (short) 21520, (short) 21521, (short) 21524, (short) 21525, (short) 21568, (short) 21569, (short) 21572, (short) 21573, (short) 21584, (short) 21585, (short) 21588, (short) 21589, (short) 21760, (short) 21761, (short) 21764, (short) 21765, (short) 21776, (short) 21777, (short) 21780, (short) 21781, (short) 21824, (short) 21825, (short) 21828, (short) 21829, (short) 21840, (short) 21841, (short) 21844, (short) 21845};
    private static final int[] INTERLEAVE3_TABLE = new int[]{0, 1, 8, 9, 64, 65, 72, 73, 512, 513, 520, 521, 576, 577, 584, 585, PKIFailureInfo.certConfirmed, 4097, 4104, 4105, 4160, 4161, 4168, 4169, 4608, 4609, 4616, 4617, 4672, 4673, 4680, 4681, 32768, 32769, 32776, 32777, 32832, 32833, 32840, 32841, 33280, 33281, 33288, 33289, 33344, 33345, 33352, 33353, 36864, 36865, 36872, 36873, 36928, 36929, 36936, 36937, 37376, 37377, 37384, 37385, 37440, 37441, 37448, 37449, PKIFailureInfo.transactionIdInUse, 262145, 262152, 262153, 262208, 262209, 262216, 262217, 262656, 262657, 262664, 262665, 262720, 262721, 262728, 262729, 266240, 266241, 266248, 266249, 266304, 266305, 266312, 266313, 266752, 266753, 266760, 266761, 266816, 266817, 266824, 266825, 294912, 294913, 294920, 294921, 294976, 294977, 294984, 294985, 295424, 295425, 295432, 295433, 295488, 295489, 295496, 295497, 299008, 299009, 299016, 299017, 299072, 299073, 299080, 299081, 299520, 299521, 299528, 299529, 299584, 299585, 299592, 299593};
    private static final int[] INTERLEAVE4_TABLE = new int[]{0, 1, 16, 17, 256, 257, 272, 273, PKIFailureInfo.certConfirmed, 4097, 4112, 4113, 4352, 4353, 4368, 4369, PKIFailureInfo.notAuthorized, 65537, 65552, 65553, 65792, 65793, 65808, 65809, 69632, 69633, 69648, 69649, 69888, 69889, 69904, 69905, PKIFailureInfo.badCertTemplate, 1048577, 1048592, 1048593, 1048832, 1048833, 1048848, 1048849, 1052672, 1052673, 1052688, 1052689, 1052928, 1052929, 1052944, 1052945, 1114112, 1114113, 1114128, 1114129, 1114368, 1114369, 1114384, 1114385, 1118208, 1118209, 1118224, 1118225, 1118464, 1118465, 1118480, 1118481, 16777216, 16777217, 16777232, 16777233, 16777472, 16777473, 16777488, 16777489, 16781312, 16781313, 16781328, 16781329, 16781568, 16781569, 16781584, 16781585, 16842752, 16842753, 16842768, 16842769, 16843008, 16843009, 16843024, 16843025, 16846848, 16846849, 16846864, 16846865, 16847104, 16847105, 16847120, 16847121, 17825792, 17825793, 17825808, 17825809, 17826048, 17826049, 17826064, 17826065, 17829888, 17829889, 17829904, 17829905, 17830144, 17830145, 17830160, 17830161, 17891328, 17891329, 17891344, 17891345, 17891584, 17891585, 17891600, 17891601, 17895424, 17895425, 17895440, 17895441, 17895680, 17895681, 17895696, 17895697, 268435456, 268435457, 268435472, 268435473, 268435712, 268435713, 268435728, 268435729, 268439552, 268439553, 268439568, 268439569, 268439808, 268439809, 268439824, 268439825, 268500992, 268500993, 268501008, 268501009, 268501248, 268501249, 268501264, 268501265, 268505088, 268505089, 268505104, 268505105, 268505344, 268505345, 268505360, 268505361, 269484032, 269484033, 269484048, 269484049, 269484288, 269484289, 269484304, 269484305, 269488128, 269488129, 269488144, 269488145, 269488384, 269488385, 269488400, 269488401, 269549568, 269549569, 269549584, 269549585, 269549824, 269549825, 269549840, 269549841, 269553664, 269553665, 269553680, 269553681, 269553920, 269553921, 269553936, 269553937, 285212672, 285212673, 285212688, 285212689, 285212928, 285212929, 285212944, 285212945, 285216768, 285216769, 285216784, 285216785, 285217024, 285217025, 285217040, 285217041, 285278208, 285278209, 285278224, 285278225, 285278464, 285278465, 285278480, 285278481, 285282304, 285282305, 285282320, 285282321, 285282560, 285282561, 285282576, 285282577, 286261248, 286261249, 286261264, 286261265, 286261504, 286261505, 286261520, 286261521, 286265344, 286265345, 286265360, 286265361, 286265600, 286265601, 286265616, 286265617, 286326784, 286326785, 286326800, 286326801, 286327040, 286327041, 286327056, 286327057, 286330880, 286330881, 286330896, 286330897, 286331136, 286331137, 286331152, 286331153};
    private static final int[] INTERLEAVE5_TABLE = new int[]{0, 1, 32, 33, 1024, 1025, 1056, 1057, 32768, 32769, 32800, 32801, 33792, 33793, 33824, 33825, PKIFailureInfo.badCertTemplate, 1048577, 1048608, 1048609, 1049600, 1049601, 1049632, 1049633, 1081344, 1081345, 1081376, 1081377, 1082368, 1082369, 1082400, 1082401, 33554432, 33554433, 33554464, 33554465, 33555456, 33555457, 33555488, 33555489, 33587200, 33587201, 33587232, 33587233, 33588224, 33588225, 33588256, 33588257, 34603008, 34603009, 34603040, 34603041, 34604032, 34604033, 34604064, 34604065, 34635776, 34635777, 34635808, 34635809, 34636800, 34636801, 34636832, 34636833, 1073741824, 1073741825, 1073741856, 1073741857, 1073742848, 1073742849, 1073742880, 1073742881, 1073774592, 1073774593, 1073774624, 1073774625, 1073775616, 1073775617, 1073775648, 1073775649, 1074790400, 1074790401, 1074790432, 1074790433, 1074791424, 1074791425, 1074791456, 1074791457, 1074823168, 1074823169, 1074823200, 1074823201, 1074824192, 1074824193, 1074824224, 1074824225, 1107296256, 1107296257, 1107296288, 1107296289, 1107297280, 1107297281, 1107297312, 1107297313, 1107329024, 1107329025, 1107329056, 1107329057, 1107330048, 1107330049, 1107330080, 1107330081, 1108344832, 1108344833, 1108344864, 1108344865, 1108345856, 1108345857, 1108345888, 1108345889, 1108377600, 1108377601, 1108377632, 1108377633, 1108378624, 1108378625, 1108378656, 1108378657};
    private static final long[] INTERLEAVE7_TABLE = new long[]{0, 1, 128, 129, 16384, 16385, 16512, 16513, 2097152, 2097153, 2097280, 2097281, 2113536, 2113537, 2113664, 2113665, 268435456, 268435457, 268435584, 268435585, 268451840, 268451841, 268451968, 268451969, 270532608, 270532609, 270532736, 270532737, 270548992, 270548993, 270549120, 270549121, 34359738368L, 34359738369L, 34359738496L, 34359738497L, 34359754752L, 34359754753L, 34359754880L, 34359754881L, 34361835520L, 34361835521L, 34361835648L, 34361835649L, 34361851904L, 34361851905L, 34361852032L, 34361852033L, 34628173824L, 34628173825L, 34628173952L, 34628173953L, 34628190208L, 34628190209L, 34628190336L, 34628190337L, 34630270976L, 34630270977L, 34630271104L, 34630271105L, 34630287360L, 34630287361L, 34630287488L, 34630287489L, 4398046511104L, 4398046511105L, 4398046511232L, 4398046511233L, 4398046527488L, 4398046527489L, 4398046527616L, 4398046527617L, 4398048608256L, 4398048608257L, 4398048608384L, 4398048608385L, 4398048624640L, 4398048624641L, 4398048624768L, 4398048624769L, 4398314946560L, 4398314946561L, 4398314946688L, 4398314946689L, 4398314962944L, 4398314962945L, 4398314963072L, 4398314963073L, 4398317043712L, 4398317043713L, 4398317043840L, 4398317043841L, 4398317060096L, 4398317060097L, 4398317060224L, 4398317060225L, 4432406249472L, 4432406249473L, 4432406249600L, 4432406249601L, 4432406265856L, 4432406265857L, 4432406265984L, 4432406265985L, 4432408346624L, 4432408346625L, 4432408346752L, 4432408346753L, 4432408363008L, 4432408363009L, 4432408363136L, 4432408363137L, 4432674684928L, 4432674684929L, 4432674685056L, 4432674685057L, 4432674701312L, 4432674701313L, 4432674701440L, 4432674701441L, 4432676782080L, 4432676782081L, 4432676782208L, 4432676782209L, 4432676798464L, 4432676798465L, 4432676798592L, 4432676798593L, 562949953421312L, 562949953421313L, 562949953421440L, 562949953421441L, 562949953437696L, 562949953437697L, 562949953437824L, 562949953437825L, 562949955518464L, 562949955518465L, 562949955518592L, 562949955518593L, 562949955534848L, 562949955534849L, 562949955534976L, 562949955534977L, 562950221856768L, 562950221856769L, 562950221856896L, 562950221856897L, 562950221873152L, 562950221873153L, 562950221873280L, 562950221873281L, 562950223953920L, 562950223953921L, 562950223954048L, 562950223954049L, 562950223970304L, 562950223970305L, 562950223970432L, 562950223970433L, 562984313159680L, 562984313159681L, 562984313159808L, 562984313159809L, 562984313176064L, 562984313176065L, 562984313176192L, 562984313176193L, 562984315256832L, 562984315256833L, 562984315256960L, 562984315256961L, 562984315273216L, 562984315273217L, 562984315273344L, 562984315273345L, 562984581595136L, 562984581595137L, 562984581595264L, 562984581595265L, 562984581611520L, 562984581611521L, 562984581611648L, 562984581611649L, 562984583692288L, 562984583692289L, 562984583692416L, 562984583692417L, 562984583708672L, 562984583708673L, 562984583708800L, 562984583708801L, 567347999932416L, 567347999932417L, 567347999932544L, 567347999932545L, 567347999948800L, 567347999948801L, 567347999948928L, 567347999948929L, 567348002029568L, 567348002029569L, 567348002029696L, 567348002029697L, 567348002045952L, 567348002045953L, 567348002046080L, 567348002046081L, 567348268367872L, 567348268367873L, 567348268368000L, 567348268368001L, 567348268384256L, 567348268384257L, 567348268384384L, 567348268384385L, 567348270465024L, 567348270465025L, 567348270465152L, 567348270465153L, 567348270481408L, 567348270481409L, 567348270481536L, 567348270481537L, 567382359670784L, 567382359670785L, 567382359670912L, 567382359670913L, 567382359687168L, 567382359687169L, 567382359687296L, 567382359687297L, 567382361767936L, 567382361767937L, 567382361768064L, 567382361768065L, 567382361784320L, 567382361784321L, 567382361784448L, 567382361784449L, 567382628106240L, 567382628106241L, 567382628106368L, 567382628106369L, 567382628122624L, 567382628122625L, 567382628122752L, 567382628122753L, 567382630203392L, 567382630203393L, 567382630203520L, 567382630203521L, 567382630219776L, 567382630219777L, 567382630219904L, 567382630219905L, 72057594037927936L, 72057594037927937L, 72057594037928064L, 72057594037928065L, 72057594037944320L, 72057594037944321L, 72057594037944448L, 72057594037944449L, 72057594040025088L, 72057594040025089L, 72057594040025216L, 72057594040025217L, 72057594040041472L, 72057594040041473L, 72057594040041600L, 72057594040041601L, 72057594306363392L, 72057594306363393L, 72057594306363520L, 72057594306363521L, 72057594306379776L, 72057594306379777L, 72057594306379904L, 72057594306379905L, 72057594308460544L, 72057594308460545L, 72057594308460672L, 72057594308460673L, 72057594308476928L, 72057594308476929L, 72057594308477056L, 72057594308477057L, 72057628397666304L, 72057628397666305L, 72057628397666432L, 72057628397666433L, 72057628397682688L, 72057628397682689L, 72057628397682816L, 72057628397682817L, 72057628399763456L, 72057628399763457L, 72057628399763584L, 72057628399763585L, 72057628399779840L, 72057628399779841L, 72057628399779968L, 72057628399779969L, 72057628666101760L, 72057628666101761L, 72057628666101888L, 72057628666101889L, 72057628666118144L, 72057628666118145L, 72057628666118272L, 72057628666118273L, 72057628668198912L, 72057628668198913L, 72057628668199040L, 72057628668199041L, 72057628668215296L, 72057628668215297L, 72057628668215424L, 72057628668215425L, 72061992084439040L, 72061992084439041L, 72061992084439168L, 72061992084439169L, 72061992084455424L, 72061992084455425L, 72061992084455552L, 72061992084455553L, 72061992086536192L, 72061992086536193L, 72061992086536320L, 72061992086536321L, 72061992086552576L, 72061992086552577L, 72061992086552704L, 72061992086552705L, 72061992352874496L, 72061992352874497L, 72061992352874624L, 72061992352874625L, 72061992352890880L, 72061992352890881L, 72061992352891008L, 72061992352891009L, 72061992354971648L, 72061992354971649L, 72061992354971776L, 72061992354971777L, 72061992354988032L, 72061992354988033L, 72061992354988160L, 72061992354988161L, 72062026444177408L, 72062026444177409L, 72062026444177536L, 72062026444177537L, 72062026444193792L, 72062026444193793L, 72062026444193920L, 72062026444193921L, 72062026446274560L, 72062026446274561L, 72062026446274688L, 72062026446274689L, 72062026446290944L, 72062026446290945L, 72062026446291072L, 72062026446291073L, 72062026712612864L, 72062026712612865L, 72062026712612992L, 72062026712612993L, 72062026712629248L, 72062026712629249L, 72062026712629376L, 72062026712629377L, 72062026714710016L, 72062026714710017L, 72062026714710144L, 72062026714710145L, 72062026714726400L, 72062026714726401L, 72062026714726528L, 72062026714726529L, 72620543991349248L, 72620543991349249L, 72620543991349376L, 72620543991349377L, 72620543991365632L, 72620543991365633L, 72620543991365760L, 72620543991365761L, 72620543993446400L, 72620543993446401L, 72620543993446528L, 72620543993446529L, 72620543993462784L, 72620543993462785L, 72620543993462912L, 72620543993462913L, 72620544259784704L, 72620544259784705L, 72620544259784832L, 72620544259784833L, 72620544259801088L, 72620544259801089L, 72620544259801216L, 72620544259801217L, 72620544261881856L, 72620544261881857L, 72620544261881984L, 72620544261881985L, 72620544261898240L, 72620544261898241L, 72620544261898368L, 72620544261898369L, 72620578351087616L, 72620578351087617L, 72620578351087744L, 72620578351087745L, 72620578351104000L, 72620578351104001L, 72620578351104128L, 72620578351104129L, 72620578353184768L, 72620578353184769L, 72620578353184896L, 72620578353184897L, 72620578353201152L, 72620578353201153L, 72620578353201280L, 72620578353201281L, 72620578619523072L, 72620578619523073L, 72620578619523200L, 72620578619523201L, 72620578619539456L, 72620578619539457L, 72620578619539584L, 72620578619539585L, 72620578621620224L, 72620578621620225L, 72620578621620352L, 72620578621620353L, 72620578621636608L, 72620578621636609L, 72620578621636736L, 72620578621636737L, 72624942037860352L, 72624942037860353L, 72624942037860480L, 72624942037860481L, 72624942037876736L, 72624942037876737L, 72624942037876864L, 72624942037876865L, 72624942039957504L, 72624942039957505L, 72624942039957632L, 72624942039957633L, 72624942039973888L, 72624942039973889L, 72624942039974016L, 72624942039974017L, 72624942306295808L, 72624942306295809L, 72624942306295936L, 72624942306295937L, 72624942306312192L, 72624942306312193L, 72624942306312320L, 72624942306312321L, 72624942308392960L, 72624942308392961L, 72624942308393088L, 72624942308393089L, 72624942308409344L, 72624942308409345L, 72624942308409472L, 72624942308409473L, 72624976397598720L, 72624976397598721L, 72624976397598848L, 72624976397598849L, 72624976397615104L, 72624976397615105L, 72624976397615232L, 72624976397615233L, 72624976399695872L, 72624976399695873L, 72624976399696000L, 72624976399696001L, 72624976399712256L, 72624976399712257L, 72624976399712384L, 72624976399712385L, 72624976666034176L, 72624976666034177L, 72624976666034304L, 72624976666034305L, 72624976666050560L, 72624976666050561L, 72624976666050688L, 72624976666050689L, 72624976668131328L, 72624976668131329L, 72624976668131456L, 72624976668131457L, 72624976668147712L, 72624976668147713L, 72624976668147840L, 72624976668147841L};
    private static final String ZEROES = "0000000000000000000000000000000000000000000000000000000000000000";
    static final byte[] bitLengths = new byte[]{(byte) 0, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3, (byte) 3, (byte) 3, (byte) 4, (byte) 4, (byte) 4, (byte) 4, (byte) 4, (byte) 4, (byte) 4, (byte) 4, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 5, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 6, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8, (byte) 8};
    private long[] m_ints;

    public LongArray(int i) {
        this.m_ints = new long[i];
    }

    public LongArray(BigInteger bigInteger) {
        if (bigInteger == null || bigInteger.signum() < 0) {
            throw new IllegalArgumentException("invalid F2m field value");
        } else if (bigInteger.signum() == 0) {
            this.m_ints = new long[]{0};
        } else {
            int i;
            int i2;
            byte[] toByteArray = bigInteger.toByteArray();
            int length = toByteArray.length;
            if (toByteArray[0] == (byte) 0) {
                length--;
                i = 1;
            } else {
                i = 0;
            }
            int i3 = (length + 7) / 8;
            this.m_ints = new long[i3];
            i3--;
            length = (length % 8) + i;
            if (i < length) {
                long j = 0;
                while (i < length) {
                    j = (j << 8) | ((long) (toByteArray[i] & 255));
                    i++;
                }
                i2 = i3 - 1;
                this.m_ints[i3] = j;
            } else {
                i2 = i3;
            }
            while (i2 >= 0) {
                length = 0;
                int i4 = i;
                long j2 = 0;
                while (length < 8) {
                    j2 = (j2 << 8) | ((long) (toByteArray[i4] & 255));
                    length++;
                    i4++;
                }
                this.m_ints[i2] = j2;
                i2--;
                i = i4;
            }
        }
    }

    public LongArray(long[] jArr) {
        this.m_ints = jArr;
    }

    public LongArray(long[] jArr, int i, int i2) {
        if (i == 0 && i2 == jArr.length) {
            this.m_ints = jArr;
            return;
        }
        this.m_ints = new long[i2];
        System.arraycopy(jArr, i, this.m_ints, 0, i2);
    }

    private static void add(long[] jArr, int i, long[] jArr2, int i2, int i3) {
        for (int i4 = 0; i4 < i3; i4++) {
            int i5 = i + i4;
            jArr[i5] = jArr[i5] ^ jArr2[i2 + i4];
        }
    }

    private static void add(long[] jArr, int i, long[] jArr2, int i2, long[] jArr3, int i3, int i4) {
        for (int i5 = 0; i5 < i4; i5++) {
            jArr3[i3 + i5] = jArr[i + i5] ^ jArr2[i2 + i5];
        }
    }

    private static void addBoth(long[] jArr, int i, long[] jArr2, int i2, long[] jArr3, int i3, int i4) {
        for (int i5 = 0; i5 < i4; i5++) {
            int i6 = i + i5;
            jArr[i6] = jArr[i6] ^ (jArr2[i2 + i5] ^ jArr3[i3 + i5]);
        }
    }

    private void addShiftedByBitsSafe(LongArray longArray, int i, int i2) {
        i = (i + 63) >>> 6;
        int i3 = i2 >>> 6;
        int i4 = i2 & 63;
        if (i4 == 0) {
            add(this.m_ints, i3, longArray.m_ints, 0, i);
            return;
        }
        long addShiftedUp = addShiftedUp(this.m_ints, i3, longArray.m_ints, 0, i, i4);
        if (addShiftedUp != 0) {
            long[] jArr = this.m_ints;
            i += i3;
            jArr[i] = addShiftedUp ^ jArr[i];
        }
    }

    private static long addShiftedDown(long[] jArr, int i, long[] jArr2, int i2, int i3, int i4) {
        int i5 = 64 - i4;
        long j = 0;
        while (true) {
            i3--;
            if (i3 < 0) {
                return j;
            }
            long j2 = jArr2[i2 + i3];
            int i6 = i + i3;
            jArr[i6] = (j | (j2 >>> i4)) ^ jArr[i6];
            j = j2 << i5;
        }
    }

    private static long addShiftedUp(long[] jArr, int i, long[] jArr2, int i2, int i3, int i4) {
        int i5 = 64 - i4;
        long j = 0;
        int i6 = i3;
        for (int i7 = 0; i7 < i6; i7++) {
            long j2 = jArr2[i2 + i7];
            int i8 = i + i7;
            jArr[i8] = (j | (j2 << i4)) ^ jArr[i8];
            j = j2 >>> i5;
        }
        return j;
    }

    private static int bitLength(long j) {
        int i = 32;
        int i2 = (int) (j >>> 32);
        if (i2 == 0) {
            i2 = (int) j;
            i = 0;
        }
        int i3 = i2 >>> 16;
        if (i3 == 0) {
            i3 = i2 >>> 8;
            i3 = i3 == 0 ? bitLengths[i2] : bitLengths[i3] + 8;
        } else {
            int i4 = i3 >>> 8;
            i3 = i4 == 0 ? 16 + bitLengths[i3] : 24 + bitLengths[i4];
        }
        return i + i3;
    }

    private int degreeFrom(int i) {
        i = (i + 62) >>> 6;
        while (i != 0) {
            i--;
            long j = this.m_ints[i];
            if (j != 0) {
                return (i << 6) + bitLength(j);
            }
        }
        return 0;
    }

    private static void distribute(long[] jArr, int i, int i2, int i3, int i4) {
        for (int i5 = 0; i5 < i4; i5++) {
            long j = jArr[i + i5];
            int i6 = i2 + i5;
            jArr[i6] = jArr[i6] ^ j;
            i6 = i3 + i5;
            jArr[i6] = j ^ jArr[i6];
        }
    }

    private static void flipBit(long[] jArr, int i, int i2) {
        i += i2 >>> 6;
        jArr[i] = jArr[i] ^ (1 << (i2 & 63));
    }

    private static void flipVector(long[] jArr, int i, long[] jArr2, int i2, int i3, int i4) {
        i += i4 >>> 6;
        i4 &= 63;
        if (i4 == 0) {
            add(jArr, i, jArr2, i2, i3);
            return;
        }
        jArr[i] = addShiftedDown(jArr, i + 1, jArr2, i2, i3, 64 - i4) ^ jArr[i];
    }

    private static void flipWord(long[] jArr, int i, int i2, long j) {
        i += i2 >>> 6;
        i2 &= 63;
        if (i2 == 0) {
            jArr[i] = jArr[i] ^ j;
            return;
        }
        jArr[i] = jArr[i] ^ (j << i2);
        long j2 = j >>> (64 - i2);
        if (j2 != 0) {
            i++;
            jArr[i] = j2 ^ jArr[i];
        }
    }

    private static void interleave(long[] jArr, int i, long[] jArr2, int i2, int i3, int i4) {
        if (i4 == 3) {
            interleave3(jArr, i, jArr2, i2, i3);
        } else if (i4 == 5) {
            interleave5(jArr, i, jArr2, i2, i3);
        } else if (i4 != 7) {
            interleave2_n(jArr, i, jArr2, i2, i3, bitLengths[i4] - 1);
        } else {
            interleave7(jArr, i, jArr2, i2, i3);
        }
    }

    private static long interleave2_32to64(int i) {
        int i2 = INTERLEAVE2_TABLE[i & 255] | (INTERLEAVE2_TABLE[(i >>> 8) & 255] << 16);
        return ((((long) ((INTERLEAVE2_TABLE[i >>> 24] << 16) | INTERLEAVE2_TABLE[(i >>> 16) & 255])) & BodyPartID.bodyIdMax) << 32) | (BodyPartID.bodyIdMax & ((long) i2));
    }

    private static long interleave2_n(long j, int i) {
        while (i > 1) {
            i -= 2;
            j = (interleave4_16to64(((int) (j >>> 48)) & 65535) << 3) | (((interleave4_16to64(((int) (j >>> 16)) & 65535) << 1) | interleave4_16to64(((int) j) & 65535)) | (interleave4_16to64(((int) (j >>> 32)) & 65535) << 2));
        }
        if (i <= 0) {
            return j;
        }
        return (interleave2_32to64((int) (j >>> 32)) << 1) | interleave2_32to64((int) j);
    }

    private static void interleave2_n(long[] jArr, int i, long[] jArr2, int i2, int i3, int i4) {
        for (int i5 = 0; i5 < i3; i5++) {
            jArr2[i2 + i5] = interleave2_n(jArr[i + i5], i4);
        }
    }

    private static long interleave3(long j) {
        return (interleave3_21to63(((int) (j >>> 42)) & 2097151) << 2) | (((Long.MIN_VALUE & j) | interleave3_21to63(((int) j) & 2097151)) | (interleave3_21to63(((int) (j >>> 21)) & 2097151) << 1));
    }

    private static void interleave3(long[] jArr, int i, long[] jArr2, int i2, int i3) {
        for (int i4 = 0; i4 < i3; i4++) {
            jArr2[i2 + i4] = interleave3(jArr[i + i4]);
        }
    }

    private static long interleave3_13to65(int i) {
        return ((((long) INTERLEAVE5_TABLE[i >>> 7]) & BodyPartID.bodyIdMax) << 35) | (BodyPartID.bodyIdMax & ((long) INTERLEAVE5_TABLE[i & CertificateBody.profileType]));
    }

    private static long interleave3_21to63(int i) {
        int i2 = INTERLEAVE3_TABLE[i & CertificateBody.profileType];
        return (((((long) INTERLEAVE3_TABLE[i >>> 14]) & BodyPartID.bodyIdMax) << 42) | ((((long) INTERLEAVE3_TABLE[(i >>> 7) & CertificateBody.profileType]) & BodyPartID.bodyIdMax) << 21)) | (((long) i2) & BodyPartID.bodyIdMax);
    }

    private static long interleave4_16to64(int i) {
        return ((((long) INTERLEAVE4_TABLE[i >>> 8]) & BodyPartID.bodyIdMax) << 32) | (BodyPartID.bodyIdMax & ((long) INTERLEAVE4_TABLE[i & 255]));
    }

    private static long interleave5(long j) {
        return (interleave3_13to65(((int) (j >>> 52)) & 8191) << 4) | (((interleave3_13to65(((int) j) & 8191) | (interleave3_13to65(((int) (j >>> 13)) & 8191) << 1)) | (interleave3_13to65(((int) (j >>> 26)) & 8191) << 2)) | (interleave3_13to65(((int) (j >>> 39)) & 8191) << 3));
    }

    private static void interleave5(long[] jArr, int i, long[] jArr2, int i2, int i3) {
        for (int i4 = 0; i4 < i3; i4++) {
            jArr2[i2 + i4] = interleave5(jArr[i + i4]);
        }
    }

    private static long interleave7(long j) {
        return (INTERLEAVE7_TABLE[((int) (j >>> 54)) & 511] << 6) | (((((((Long.MIN_VALUE & j) | INTERLEAVE7_TABLE[((int) j) & 511]) | (INTERLEAVE7_TABLE[((int) (j >>> 9)) & 511] << 1)) | (INTERLEAVE7_TABLE[((int) (j >>> 18)) & 511] << 2)) | (INTERLEAVE7_TABLE[((int) (j >>> 27)) & 511] << 3)) | (INTERLEAVE7_TABLE[((int) (j >>> 36)) & 511] << 4)) | (INTERLEAVE7_TABLE[((int) (j >>> 45)) & 511] << 5));
    }

    private static void interleave7(long[] jArr, int i, long[] jArr2, int i2, int i3) {
        for (int i4 = 0; i4 < i3; i4++) {
            jArr2[i2 + i4] = interleave7(jArr[i + i4]);
        }
    }

    private static void multiplyWord(long j, long[] jArr, int i, long[] jArr2, int i2) {
        long[] jArr3;
        int i3 = i;
        long[] jArr4 = jArr2;
        int i4 = i2;
        if ((j & 1) != 0) {
            jArr3 = jArr;
            add(jArr4, i4, jArr3, 0, i3);
        } else {
            jArr3 = jArr;
        }
        long j2 = j;
        int i5 = 1;
        while (true) {
            long j3 = j2 >>> 1;
            if (j3 != 0) {
                if ((j3 & 1) != 0) {
                    j2 = addShiftedUp(jArr4, i4, jArr3, 0, i3, i5);
                    if (j2 != 0) {
                        int i6 = i4 + i3;
                        jArr4[i6] = j2 ^ jArr4[i6];
                    }
                }
                i5++;
                j2 = j3;
            } else {
                return;
            }
        }
    }

    private static void reduceBit(long[] jArr, int i, int i2, int i3, int[] iArr) {
        flipBit(jArr, i, i2);
        i2 -= i3;
        i3 = iArr.length;
        while (true) {
            i3--;
            if (i3 >= 0) {
                flipBit(jArr, i, iArr[i3] + i2);
            } else {
                flipBit(jArr, i, i2);
                return;
            }
        }
    }

    private static void reduceBitWise(long[] jArr, int i, int i2, int i3, int[] iArr) {
        while (true) {
            i2--;
            if (i2 < i3) {
                return;
            }
            if (testBit(jArr, i, i2)) {
                reduceBit(jArr, i, i2, i3, iArr);
            }
        }
    }

    private static int reduceInPlace(long[] jArr, int i, int i2, int i3, int[] iArr) {
        long[] jArr2 = jArr;
        int i4 = i;
        int i5 = i2;
        int i6 = i3;
        int[] iArr2 = iArr;
        int i7 = (i6 + 63) >>> 6;
        if (i5 < i7) {
            return i5;
        }
        int i8 = i5 << 6;
        int min = Math.min(i8, (i6 << 1) - 1);
        i8 -= min;
        int i9 = i5;
        while (i8 >= 64) {
            i9--;
            i8 -= 64;
        }
        int length = iArr2.length;
        int i10 = iArr2[length - 1];
        length = length > 1 ? iArr2[length - 2] : 0;
        int max = Math.max(i6, i10 + 64);
        i5 = (i8 + Math.min(min - max, i6 - length)) >> 6;
        if (i5 > 1) {
            int i11 = i9 - i5;
            reduceVectorWise(jArr2, i4, i9, i11, i6, iArr2);
            while (i9 > i11) {
                i9--;
                jArr2[i4 + i9] = 0;
            }
            min = i11 << 6;
        }
        if (min > max) {
            reduceWordWise(jArr2, i4, i9, max, i6, iArr2);
        } else {
            max = min;
        }
        if (max > i6) {
            reduceBitWise(jArr2, i4, max, i6, iArr2);
        }
        return i7;
    }

    private static LongArray reduceResult(long[] jArr, int i, int i2, int i3, int[] iArr) {
        return new LongArray(jArr, i, reduceInPlace(jArr, i, i2, i3, iArr));
    }

    private static void reduceVectorWise(long[] jArr, int i, int i2, int i3, int i4, int[] iArr) {
        int i5 = (i3 << 6) - i4;
        i4 = iArr.length;
        while (true) {
            i4--;
            if (i4 >= 0) {
                flipVector(jArr, i, jArr, i + i3, i2 - i3, i5 + iArr[i4]);
            } else {
                flipVector(jArr, i, jArr, i + i3, i2 - i3, i5);
                return;
            }
        }
    }

    private static void reduceWord(long[] jArr, int i, int i2, long j, int i3, int[] iArr) {
        i2 -= i3;
        i3 = iArr.length;
        while (true) {
            i3--;
            if (i3 >= 0) {
                flipWord(jArr, i, iArr[i3] + i2, j);
            } else {
                flipWord(jArr, i, i2, j);
                return;
            }
        }
    }

    private static void reduceWordWise(long[] jArr, int i, int i2, int i3, int i4, int[] iArr) {
        int i5;
        int i6 = i3 >>> 6;
        int i7 = i2;
        while (true) {
            int i8 = i7 - 1;
            if (i8 <= i6) {
                break;
            }
            i5 = i + i8;
            long j = jArr[i5];
            if (j != 0) {
                jArr[i5] = 0;
                reduceWord(jArr, i, i8 << 6, j, i4, iArr);
            }
            i7 = i8;
        }
        i5 = i3 & 63;
        int i9 = i + i6;
        long j2 = jArr[i9] >>> i5;
        if (j2 != 0) {
            jArr[i9] = jArr[i9] ^ (j2 << i5);
            reduceWord(jArr, i, i3, j2, i4, iArr);
        }
    }

    private long[] resizedInts(int i) {
        long[] jArr = new long[i];
        System.arraycopy(this.m_ints, 0, jArr, 0, Math.min(this.m_ints.length, i));
        return jArr;
    }

    private static long shiftUp(long[] jArr, int i, int i2, int i3) {
        int i4 = 64 - i3;
        long j = 0;
        for (int i5 = 0; i5 < i2; i5++) {
            int i6 = i + i5;
            long j2 = jArr[i6];
            jArr[i6] = j | (j2 << i3);
            j = j2 >>> i4;
        }
        return j;
    }

    private static long shiftUp(long[] jArr, int i, long[] jArr2, int i2, int i3, int i4) {
        int i5 = 64 - i4;
        long j = 0;
        for (int i6 = 0; i6 < i3; i6++) {
            long j2 = jArr[i + i6];
            jArr2[i2 + i6] = j | (j2 << i4);
            j = j2 >>> i5;
        }
        return j;
    }

    private static void squareInPlace(long[] jArr, int i, int i2, int[] iArr) {
        i2 = i << 1;
        while (true) {
            i--;
            if (i >= 0) {
                long j = jArr[i];
                i2--;
                jArr[i2] = interleave2_32to64((int) (j >>> 32));
                i2--;
                jArr[i2] = interleave2_32to64((int) j);
            } else {
                return;
            }
        }
    }

    private static boolean testBit(long[] jArr, int i, int i2) {
        return (jArr[i + (i2 >>> 6)] & (1 << (i2 & 63))) != 0;
    }

    public LongArray addOne() {
        if (this.m_ints.length == 0) {
            return new LongArray(new long[]{1});
        }
        long[] resizedInts = resizedInts(Math.max(1, getUsedLength()));
        resizedInts[0] = 1 ^ resizedInts[0];
        return new LongArray(resizedInts);
    }

    public void addShiftedByWords(LongArray longArray, int i) {
        int usedLength = longArray.getUsedLength();
        if (usedLength != 0) {
            int i2 = usedLength + i;
            if (i2 > this.m_ints.length) {
                this.m_ints = resizedInts(i2);
            }
            add(this.m_ints, i, longArray.m_ints, 0, usedLength);
        }
    }

    public Object clone() {
        return new LongArray(Arrays.clone(this.m_ints));
    }

    void copyTo(long[] jArr, int i) {
        System.arraycopy(this.m_ints, 0, jArr, i, this.m_ints.length);
    }

    public int degree() {
        int length = this.m_ints.length;
        while (length != 0) {
            length--;
            long j = this.m_ints[length];
            if (j != 0) {
                return (length << 6) + bitLength(j);
            }
        }
        return 0;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LongArray)) {
            return false;
        }
        LongArray longArray = (LongArray) obj;
        int usedLength = getUsedLength();
        if (longArray.getUsedLength() != usedLength) {
            return false;
        }
        for (int i = 0; i < usedLength; i++) {
            if (this.m_ints[i] != longArray.m_ints[i]) {
                return false;
            }
        }
        return true;
    }

    public int getLength() {
        return this.m_ints.length;
    }

    public int getUsedLength() {
        return getUsedLengthFrom(this.m_ints.length);
    }

    public int getUsedLengthFrom(int i) {
        long[] jArr = this.m_ints;
        i = Math.min(i, jArr.length);
        if (i < 1) {
            return 0;
        }
        if (jArr[0] != 0) {
            while (true) {
                i--;
                if (jArr[i] != 0) {
                    return i + 1;
                }
            }
        } else {
            do {
                i--;
                if (jArr[i] != 0) {
                    return i + 1;
                }
            } while (i > 0);
            return 0;
        }
    }

    public int hashCode() {
        int usedLength = getUsedLength();
        int i = 1;
        for (int i2 = 0; i2 < usedLength; i2++) {
            long j = this.m_ints[i2];
            i = (((i * 31) ^ ((int) j)) * 31) ^ ((int) (j >>> 32));
        }
        return i;
    }

    public boolean isOne() {
        long[] jArr = this.m_ints;
        if (jArr[0] != 1) {
            return false;
        }
        for (int i = 1; i < jArr.length; i++) {
            if (jArr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isZero() {
        long[] jArr = this.m_ints;
        for (long j : jArr) {
            if (j != 0) {
                return false;
            }
        }
        return true;
    }

    public LongArray modInverse(int i, int[] iArr) {
        int degree = degree();
        if (degree == 0) {
            throw new IllegalStateException();
        } else if (degree == 1) {
            return this;
        } else {
            LongArray longArray = (LongArray) clone();
            int i2 = (i + 63) >>> 6;
            reduceBit(new LongArray(i2).m_ints, 0, i, i, iArr);
            new LongArray(i2).m_ints[0] = 1;
            LongArray longArray2 = new LongArray(i2);
            int[] iArr2 = new int[]{degree, i + 1};
            LongArray[] longArrayArr = new LongArray[]{longArray, r4};
            int[] iArr3 = new int[]{1, 0};
            LongArray[] longArrayArr2 = new LongArray[]{r13, longArray2};
            int i3 = iArr2[1];
            int i4 = i3 - iArr2[0];
            int i5 = iArr3[1];
            i2 = i3;
            i3 = 1;
            while (true) {
                if (i4 < 0) {
                    i4 = -i4;
                    iArr2[i3] = i2;
                    iArr3[i3] = i5;
                    i3 = 1 - i3;
                    i2 = iArr2[i3];
                    i5 = iArr3[i3];
                }
                int i6 = 1 - i3;
                longArrayArr[i3].addShiftedByBitsSafe(longArrayArr[i6], iArr2[i6], i4);
                int degreeFrom = longArrayArr[i3].degreeFrom(i2);
                if (degreeFrom == 0) {
                    return longArrayArr2[i6];
                }
                int i7 = iArr3[i6];
                longArrayArr2[i3].addShiftedByBitsSafe(longArrayArr2[i6], i7, i4);
                i7 += i4;
                if (i7 > i5) {
                    i5 = i7;
                } else if (i7 == i5) {
                    i5 = longArrayArr2[i3].degreeFrom(i5);
                }
                i4 += degreeFrom - i2;
                i2 = degreeFrom;
            }
        }
    }

    public LongArray modMultiply(LongArray longArray, int i, int[] iArr) {
        int i2 = i;
        int[] iArr2 = iArr;
        int degree = degree();
        if (degree == 0) {
            return this;
        }
        int degree2 = longArray.degree();
        if (degree2 == 0) {
            return longArray;
        }
        LongArray longArray2;
        LongArray longArray3;
        if (degree > degree2) {
            longArray2 = this;
            longArray3 = longArray;
            int i3 = degree2;
            degree2 = degree;
            degree = i3;
        } else {
            longArray3 = this;
            longArray2 = longArray;
        }
        int i4 = (degree + 63) >>> 6;
        int i5 = (degree2 + 63) >>> 6;
        degree = ((degree + degree2) + 62) >>> 6;
        long[] jArr;
        if (i4 == 1) {
            long j = longArray3.m_ints[0];
            if (j == 1) {
                return longArray2;
            }
            jArr = new long[degree];
            multiplyWord(j, longArray2.m_ints, i5, jArr, 0);
            return reduceResult(jArr, 0, degree, i2, iArr2);
        }
        long[] jArr2;
        degree2 = ((degree2 + 7) + 63) >>> 6;
        int[] iArr3 = new int[16];
        long[] jArr3 = new long[(degree2 << 4)];
        iArr3[1] = degree2;
        System.arraycopy(longArray2.m_ints, 0, jArr3, degree2, i5);
        int i6 = 2;
        int i7 = degree2;
        while (i6 < 16) {
            i7 += degree2;
            iArr3[i6] = i7;
            if ((i6 & 1) == 0) {
                jArr2 = jArr3;
                shiftUp(jArr3, i7 >>> 1, jArr3, i7, degree2, 1);
            } else {
                jArr2 = jArr3;
                add(jArr2, degree2, jArr2, i7 - degree2, jArr2, i7, degree2);
            }
            i6++;
            jArr3 = jArr2;
        }
        jArr2 = jArr3;
        long[] jArr4 = new long[jArr2.length];
        shiftUp(jArr2, 0, jArr4, 0, jArr2.length, 4);
        jArr = longArray3.m_ints;
        long[] jArr5 = new long[(degree << 3)];
        for (int i8 = 0; i8 < i4; i8++) {
            long j2 = jArr[i8];
            int i9 = i8;
            while (true) {
                j2 >>>= 4;
                int i10 = ((int) j2) & 15;
                long j3 = j2;
                addBoth(jArr5, i9, jArr2, iArr3[((int) j2) & 15], jArr4, iArr3[i10], degree2);
                j2 = j3 >>> 4;
                if (j2 == 0) {
                    break;
                }
                i9 += degree;
            }
        }
        int length = jArr5.length;
        while (true) {
            length -= degree;
            if (length == 0) {
                return reduceResult(jArr5, 0, degree, i2, iArr2);
            }
            addShiftedUp(jArr5, length - degree, jArr5, length, degree, 8);
        }
    }

    public LongArray modMultiplyAlt(LongArray longArray, int i, int[] iArr) {
        int i2 = i;
        int[] iArr2 = iArr;
        int degree = degree();
        if (degree == 0) {
            return this;
        }
        int degree2 = longArray.degree();
        if (degree2 == 0) {
            return longArray;
        }
        LongArray longArray2;
        LongArray longArray3;
        if (degree > degree2) {
            longArray2 = this;
            longArray3 = longArray;
            int i3 = degree2;
            degree2 = degree;
            degree = i3;
        } else {
            longArray3 = this;
            longArray2 = longArray;
        }
        int i4 = (degree + 63) >>> 6;
        int i5 = (degree2 + 63) >>> 6;
        degree = ((degree + degree2) + 62) >>> 6;
        if (i4 == 1) {
            long j = longArray3.m_ints[0];
            if (j == 1) {
                return longArray2;
            }
            long[] jArr = new long[degree];
            multiplyWord(j, longArray2.m_ints, i5, jArr, 0);
            return reduceResult(jArr, 0, degree, i2, iArr2);
        }
        int i6 = 15;
        degree2 = ((degree2 + 15) + 63) >>> 6;
        int i7 = degree2 * 8;
        int[] iArr3 = new int[16];
        iArr3[0] = i4;
        int i8 = i4 + i7;
        iArr3[1] = i8;
        int i9 = i8;
        for (i8 = 2; i8 < iArr3.length; i8++) {
            i9 += degree;
            iArr3[i8] = i9;
        }
        Object obj = new long[((i9 + degree) + 1)];
        int[] iArr4 = iArr3;
        int i10 = i7;
        interleave(longArray3.m_ints, 0, obj, 0, i4, 4);
        System.arraycopy(longArray2.m_ints, 0, obj, i4, i5);
        int i11 = i4;
        i5 = 1;
        while (i5 < 8) {
            i9 = i11 + degree2;
            shiftUp(obj, i4, obj, i9, degree2, i5);
            i5++;
            i11 = i9;
        }
        long j2 = 0;
        while (true) {
            i8 = 0;
            do {
                long j3 = obj[i8] >>> j2;
                int i12 = i4;
                int i13 = 0;
                while (true) {
                    i5 = ((int) j3) & i6;
                    if (i5 != 0) {
                        add(obj, iArr4[i5] + i8, obj, i12, degree2);
                    }
                    i13++;
                    if (i13 == 8) {
                        break;
                    }
                    int i14 = i10;
                    i12 += degree2;
                    j3 >>>= 4;
                }
                i8++;
            } while (i8 < i4);
            j2 += 32;
            if (j2 >= 64) {
                if (j2 >= 64) {
                    break;
                }
                j2 = 60;
                i6 &= i6 << 4;
            }
            int i15 = i10;
            shiftUp(obj, i4, i15, 8);
            i10 = i15;
        }
        degree2 = iArr4.length;
        while (true) {
            degree2--;
            if (degree2 <= 1) {
                return reduceResult(obj, iArr4[1], degree, i2, iArr2);
            }
            if ((((long) degree2) & 1) == 0) {
                addShiftedUp(obj, iArr4[degree2 >>> 1], obj, iArr4[degree2], degree, 16);
            } else {
                distribute(obj, iArr4[degree2], iArr4[degree2 - 1], iArr4[1], degree);
            }
        }
    }

    public LongArray modMultiplyLD(LongArray longArray, int i, int[] iArr) {
        int i2 = i;
        int[] iArr2 = iArr;
        int degree = degree();
        if (degree == 0) {
            return this;
        }
        int degree2 = longArray.degree();
        if (degree2 == 0) {
            return longArray;
        }
        LongArray longArray2;
        LongArray longArray3;
        if (degree > degree2) {
            longArray2 = this;
            longArray3 = longArray;
            int i3 = degree2;
            degree2 = degree;
            degree = i3;
        } else {
            longArray3 = this;
            longArray2 = longArray;
        }
        int i4 = (degree + 63) >>> 6;
        int i5 = (degree2 + 63) >>> 6;
        degree = ((degree + degree2) + 62) >>> 6;
        int i6 = 1;
        long[] jArr;
        if (i4 == 1) {
            long j = longArray3.m_ints[0];
            if (j == 1) {
                return longArray2;
            }
            jArr = new long[degree];
            multiplyWord(j, longArray2.m_ints, i5, jArr, 0);
            return reduceResult(jArr, 0, degree, i2, iArr2);
        }
        int i7;
        int i8;
        int i9;
        degree2 = ((degree2 + 7) + 63) >>> 6;
        int[] iArr3 = new int[16];
        long[] jArr2 = new long[(degree2 << 4)];
        iArr3[1] = degree2;
        System.arraycopy(longArray2.m_ints, 0, jArr2, degree2, i5);
        int i10 = 2;
        i5 = degree2;
        while (i10 < 16) {
            long[] jArr3;
            i5 += degree2;
            iArr3[i10] = i5;
            if ((i10 & 1) == 0) {
                jArr3 = jArr2;
                shiftUp(jArr2, i5 >>> 1, jArr2, i5, degree2, 1);
            } else {
                jArr3 = jArr2;
                add(jArr3, degree2, jArr3, i5 - degree2, jArr3, i5, degree2);
            }
            i10++;
            jArr2 = jArr3;
        }
        long[] jArr4 = jArr2;
        long[] jArr5 = new long[jArr4.length];
        shiftUp(jArr4, 0, jArr5, 0, jArr4.length, 4);
        jArr = longArray3.m_ints;
        long[] jArr6 = new long[degree];
        int i11 = 56;
        int i12 = 56;
        while (true) {
            i7 = 8;
            if (i12 < 0) {
                break;
            }
            int i13 = i6;
            while (i13 < i4) {
                i8 = (int) (jArr[i13] >>> i12);
                i9 = i8 & 15;
                i8 = (i8 >>> 4) & 15;
                int i14 = i13;
                i6 = i7;
                addBoth(jArr6, i13 - 1, jArr4, iArr3[i9], jArr5, iArr3[i8], degree2);
                i13 = i14 + 2;
                i7 = i6;
            }
            shiftUp(jArr6, 0, degree, i7);
            i12 -= 8;
            i6 = 1;
        }
        i6 = 8;
        while (i11 >= 0) {
            i7 = 0;
            while (i7 < i4) {
                i8 = (int) (jArr[i7] >>> i11);
                i9 = i8 & 15;
                i8 = (i8 >>> 4) & 15;
                i12 = i7;
                addBoth(jArr6, i7, jArr4, iArr3[i9], jArr5, iArr3[i8], degree2);
                i7 = i12 + 2;
            }
            if (i11 > 0) {
                shiftUp(jArr6, 0, degree, i6);
            }
            i11 -= 8;
        }
        return reduceResult(jArr6, 0, degree, i2, iArr2);
    }

    public LongArray modReduce(int i, int[] iArr) {
        long[] clone = Arrays.clone(this.m_ints);
        return new LongArray(clone, 0, reduceInPlace(clone, 0, clone.length, i, iArr));
    }

    public LongArray modSquare(int i, int[] iArr) {
        int usedLength = getUsedLength();
        if (usedLength == 0) {
            return this;
        }
        usedLength <<= 1;
        long[] jArr = new long[usedLength];
        int i2 = 0;
        while (i2 < usedLength) {
            long j = this.m_ints[i2 >>> 1];
            int i3 = i2 + 1;
            jArr[i2] = interleave2_32to64((int) j);
            i2 = i3 + 1;
            jArr[i3] = interleave2_32to64((int) (j >>> 32));
        }
        return new LongArray(jArr, 0, reduceInPlace(jArr, 0, jArr.length, i, iArr));
    }

    public LongArray modSquareN(int i, int i2, int[] iArr) {
        int usedLength = getUsedLength();
        if (usedLength == 0) {
            return this;
        }
        long[] jArr = new long[(((i2 + 63) >>> 6) << 1)];
        System.arraycopy(this.m_ints, 0, jArr, 0, usedLength);
        while (true) {
            i--;
            if (i < 0) {
                return new LongArray(jArr, 0, usedLength);
            }
            squareInPlace(jArr, usedLength, i2, iArr);
            usedLength = reduceInPlace(jArr, 0, jArr.length, i2, iArr);
        }
    }

    public LongArray multiply(LongArray longArray, int i, int[] iArr) {
        int degree = degree();
        if (degree == 0) {
            return this;
        }
        int degree2 = longArray.degree();
        if (degree2 == 0) {
            return longArray;
        }
        LongArray longArray2;
        LongArray longArray3;
        if (degree > degree2) {
            longArray2 = this;
            longArray3 = longArray;
            int i2 = degree2;
            degree2 = degree;
            degree = i2;
        } else {
            longArray3 = this;
            longArray2 = longArray;
        }
        int i3 = (degree + 63) >>> 6;
        int i4 = (degree2 + 63) >>> 6;
        degree = ((degree + degree2) + 62) >>> 6;
        long[] jArr;
        if (i3 == 1) {
            long j = longArray3.m_ints[0];
            if (j == 1) {
                return longArray2;
            }
            jArr = new long[degree];
            multiplyWord(j, longArray2.m_ints, i4, jArr, 0);
            return new LongArray(jArr, 0, degree);
        }
        degree2 = ((degree2 + 7) + 63) >>> 6;
        int[] iArr2 = new int[16];
        long[] jArr2 = new long[(degree2 << 4)];
        iArr2[1] = degree2;
        System.arraycopy(longArray2.m_ints, 0, jArr2, degree2, i4);
        int i5 = 2;
        int i6 = degree2;
        while (i5 < 16) {
            long[] jArr3;
            i6 += degree2;
            iArr2[i5] = i6;
            if ((i5 & 1) == 0) {
                jArr3 = jArr2;
                shiftUp(jArr2, i6 >>> 1, jArr2, i6, degree2, 1);
            } else {
                jArr3 = jArr2;
                add(jArr3, degree2, jArr3, i6 - degree2, jArr3, i6, degree2);
            }
            i5++;
            jArr2 = jArr3;
        }
        long[] jArr4 = jArr2;
        long[] jArr5 = new long[jArr4.length];
        shiftUp(jArr4, 0, jArr5, 0, jArr4.length, 4);
        jArr = longArray3.m_ints;
        long[] jArr6 = new long[(degree << 3)];
        for (i4 = 0; i4 < i3; i4++) {
            long j2 = jArr[i4];
            int i7 = i4;
            while (true) {
                long j3 = j2 >>> 4;
                int i8 = ((int) j3) & 15;
                long j4 = j3;
                addBoth(jArr6, i7, jArr4, iArr2[((int) j2) & 15], jArr5, iArr2[i8], degree2);
                j2 = j4 >>> 4;
                if (j2 == 0) {
                    break;
                }
                i7 += degree;
            }
        }
        int length = jArr6.length;
        while (true) {
            length -= degree;
            if (length == 0) {
                return new LongArray(jArr6, 0, degree);
            }
            addShiftedUp(jArr6, length - degree, jArr6, length, degree, 8);
        }
    }

    public void reduce(int i, int[] iArr) {
        long[] jArr = this.m_ints;
        i = reduceInPlace(jArr, 0, jArr.length, i, iArr);
        if (i < jArr.length) {
            this.m_ints = new long[i];
            System.arraycopy(jArr, 0, this.m_ints, 0, i);
        }
    }

    public LongArray square(int i, int[] iArr) {
        i = getUsedLength();
        if (i == 0) {
            return this;
        }
        i <<= 1;
        long[] jArr = new long[i];
        int i2 = 0;
        while (i2 < i) {
            long j = this.m_ints[i2 >>> 1];
            int i3 = i2 + 1;
            jArr[i2] = interleave2_32to64((int) j);
            i2 = i3 + 1;
            jArr[i3] = interleave2_32to64((int) (j >>> 32));
        }
        return new LongArray(jArr, 0, jArr.length);
    }

    public boolean testBitZero() {
        return this.m_ints.length > 0 && (this.m_ints[0] & 1) != 0;
    }

    public BigInteger toBigInteger() {
        int usedLength = getUsedLength();
        if (usedLength == 0) {
            return ECConstants.ZERO;
        }
        int i = usedLength - 1;
        long j = this.m_ints[i];
        byte[] bArr = new byte[8];
        int i2 = 0;
        int i3 = 0;
        int i4 = i3;
        for (int i5 = 7; i5 >= 0; i5--) {
            byte b = (byte) ((int) (j >>> (8 * i5)));
            if (i3 != 0 || b != (byte) 0) {
                i3 = i4 + 1;
                bArr[i4] = b;
                i4 = i3;
                i3 = 1;
            }
        }
        byte[] bArr2 = new byte[((i * 8) + i4)];
        while (i2 < i4) {
            bArr2[i2] = bArr[i2];
            i2++;
        }
        for (usedLength -= 2; usedLength >= 0; usedLength--) {
            j = this.m_ints[usedLength];
            int i6 = 7;
            while (i6 >= 0) {
                i2 = i4 + 1;
                bArr2[i4] = (byte) ((int) (j >>> (8 * i6)));
                i6--;
                i4 = i2;
            }
        }
        return new BigInteger(1, bArr2);
    }

    public String toString() {
        int usedLength = getUsedLength();
        if (usedLength == 0) {
            return "0";
        }
        usedLength--;
        StringBuffer stringBuffer = new StringBuffer(Long.toBinaryString(this.m_ints[usedLength]));
        while (true) {
            usedLength--;
            if (usedLength < 0) {
                return stringBuffer.toString();
            }
            String toBinaryString = Long.toBinaryString(this.m_ints[usedLength]);
            int length = toBinaryString.length();
            if (length < 64) {
                stringBuffer.append(ZEROES.substring(length));
            }
            stringBuffer.append(toBinaryString);
        }
    }
}
