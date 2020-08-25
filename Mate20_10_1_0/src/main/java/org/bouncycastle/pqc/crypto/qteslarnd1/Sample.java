package org.bouncycastle.pqc.crypto.qteslarnd1;

import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.util.Arrays;

class Sample {
    static final long[][] CUMULATIVE_DISTRIBUTION_TABLE_I = {new long[]{144115188075855872L, 0}, new long[]{216172782113783808L, 0}, new long[]{225179981368524800L, 0}, new long[]{225461456345235456L, 0}, new long[]{225463655368491008L, 0}, new long[]{225463659663458304L, 0}, new long[]{225463659665555456L, 0}, new long[]{225463659665555712L, 0}, new long[]{225463659665555712L, 144115188075855872L}, new long[]{225463659665555712L, 144116287587483648L}, new long[]{225463659665555712L, 144116287589580800L}, new long[]{225463659665555712L, 144116287589580801L}};
    static final long[][] CUMULATIVE_DISTRIBUTION_TABLE_III = {new long[]{2199023255552L, 0, 0}, new long[]{3298534883328L, 0, 0}, new long[]{3435973836800L, 0, 0}, new long[]{3440268804096L, 0, 0}, new long[]{3440302358528L, 0, 0}, new long[]{3440302424064L, 0, 0}, new long[]{3440302424096L, 0, 0}, new long[]{3440302424096L, 72057594037927936L, 0}, new long[]{3440302424096L, 72059793061183488L, 0}, new long[]{3440302424096L, 72059793077960704L, 0}, new long[]{3440302424096L, 72059793077960736L, 0}, new long[]{3440302424096L, 72059793077960736L, 281474976710656L}, new long[]{3440302424096L, 72059793077960736L, 281475010265088L}, new long[]{3440302424096L, 72059793077960736L, 281475010265089L}};
    static final double[][] EXPONENTIAL_DISTRIBUTION_I = {new double[]{1.0d, 0.9990496327075997d, 0.99810016861319d, 0.9971516068584009d, 0.9962039465856783d, 0.9952571869382832d, 0.9943113270602909d, 0.9933663660965897d, 0.992422303192881d, 0.9914791374956781d, 0.9905368681523049d, 0.9895954943108964d, 0.9886550151203967d, 0.9877154297305588d, 0.9867767372919439d, 0.9858389369559202d, 0.9849020278746626d, 0.983966009201152d, 0.9830308800891736d, 0.9820966396933174d, 0.9811632871689767d, 0.9802308216723474d, 0.9792992423604274d, 0.9783685483910157d, 0.9774387389227118d, 0.9765098131149148d, 0.9755817701278225d, 0.9746546091224311d, 0.973728329260534d, 0.9728029297047213d, 0.9718784096183788d, 0.9709547681656876d}, new double[]{1.0d, 0.9700320045116229d, 0.940962089776837d, 0.9127633421156709d, 0.8854096543971924d, 0.8588757018688518d, 0.8331369187101693d, 0.8081694752890625d, 0.7839502560997557d, 0.7604568383618461d, 0.7376674712607126d, 0.715561055810049d, 0.6941171253178751d, 0.6733158264379437d, 0.6531379007889985d, 0.6335646671248656d, 0.6145780040388725d, 0.5961603331865797d, 0.5782946030112949d, 0.5609642729572996d, 0.5441532981561744d, 0.5278461145720446d, 0.5120276245919921d, 0.49668318304829484d, 0.48179858365955075d, 0.46736004587813484d, 0.45335420213181116d, 0.4397680854476882d, 0.42658911744705963d, 0.4138050967000153d, 0.40140418742904177d, 0.3893749085511526d}, new double[]{1.0d, 0.37770612304840434d, 0.14266191538825637d, 0.05388427896795781d, 0.02035242210224602d, 0.007687234446884d, 0.0029035155198967005d, 0.0010966755902310549d, 4.142210854279923E-4d, 1.5645384026190888E-4d, 5.9093573441359953E-5d, 2.232000452161222E-5d, 8.430402374281007E-6d, 3.184214596527742E-6d, 1.2026973502086328E-6d, 4.5426615334789165E-7d, 1.715791076131441E-7d, 6.480647953266561E-8d, 2.4477804132698898E-8d, 9.24541649969991E-9d, 3.4920504220694024E-9d, 1.318968826409378E-9d, 4.9818260184479E-10d, 1.881666191129625E-10d, 7.107168419228285E-11d, 2.6844210294787717E-11d, 1.0139222596740334E-11d, 3.829646457739566E-12d, 1.4464809161988664E-12d, 5.463446989209777E-13d, 2.0635773807749023E-13d, 7.794258121028692E-14d}};
    static final double[][] EXPONENTIAL_DISTRIBUTION_III_SIZE = {new double[]{1.0d, 0.9914791374956781d, 0.9830308800891736d, 0.9746546091224311d, 0.9663497112088952d, 0.9581155781885929d, 0.9499516070835989d, 0.94185720005388d, 0.9338317643535151d, 0.9258747122872905d, 0.9179854611676618d, 0.9101634332720855d, 0.9024080558007124d, 0.894718760834442d, 0.8870949852933344d, 0.8795361708953764d, 0.872041764115599d, 0.8646112161455436d, 0.8572439828530728d, 0.8499395247425244d, 0.8426973069152046d, 0.8355167990302177d, 0.82839747526563d, 0.8213388142799641d, 0.8143402991740217d, 0.8074014174530314d, 0.8005216609891195d, 0.7937005259840998d, 0.7869375129325812d, 0.7802321265853895d, 0.7735838759133007d, 0.766992274071083d}, new double[]{1.0d, 0.7604568383618461d, 0.5782946030112949d, 0.4397680854476882d, 0.3344246478719911d, 0.254315510391008d, 0.19339596897832517d, 0.14706928712118283d, 0.11183984510430525d, 0.08504937501089856d, 0.06467637882543892d, 0.049183594558286324d, 0.037402000817065316d, 0.02844260728975267d, 0.021629375214332912d, 0.016448206291233683d, 0.012508150952954992d, 0.009511908927436865d, 0.007233396189744457d, 0.005500685597071693d, 0.004183033977971684d, 0.0031810167936485224d, 0.002419025973673892d, 0.0018395648438552343d, 0.0013989096651197545d, 0.0010638104210907973d, 8.089819094390918E-4d, 6.15195825143981E-4d, 4.678298721623989E-4d, 3.557644254758445E-4d, 2.7054349019897927E-4d, 2.0573664719609488E-4d}, new double[]{1.0d, 1.5645384026190888E-4d, 2.4477804132698898E-8d, 3.829646457739566E-12d, 5.991628951587712E-16d, 9.374133589003324E-20d, 1.4666191991277205E-23d, 2.294582059053771E-27d, 3.5899617493504067E-31d, 5.616633020792314E-35d, 8.787438054448035E-39d, 1.3748284296820321E-42d, 2.1509718752500368E-46d, 3.365278101782278E-50d, 5.265106825731444E-54d, 8.237461822748735E-58d, 1.2887825361799032E-61d, 2.016349770478284E-65d, 3.15465664902546E-69d, 4.93558147447798E-73d, 7.721906756076147E-77d, 1.2081219661324923E-80d, 1.8901532110619624E-84d, 2.957217285540224E-88d, 4.626680008116659E-92d, 7.238618549328511E-96d, 1.1325096702335332E-99d, 1.7718548704178432E-103d, 2.7721349886363846E-107d, 4.337111646965655E-111d, 6.78557772812429E-115d, 1.0616296939607244E-118d}};
    static final double[][] EXPONENTIAL_DISTRIBUTION_III_SPEED = {new double[]{1.0d, 0.9951980443443538d, 0.9904191474668262d, 0.9856631986401876d, 0.980930087668915d, 0.9762197048866396d, 0.9715319411536059d, 0.9668666878541423d, 0.9622238368941451d, 0.9576032806985737d, 0.9530049122089577d, 0.9484286248809173d, 0.9438743126816935d, 0.9393418700876924d, 0.9348311920820395d, 0.9303421741521466d, 0.9258747122872905d, 0.9214287029762026d, 0.9170040432046712d, 0.9126006304531541d, 0.9082183626944031d, 0.903857138391101d, 0.8995168564935077d, 0.8951974164371195d, 0.8908987181403393d, 0.8866206620021573d, 0.8823631488998432d, 0.8781260801866497d, 0.8739093576895269d, 0.8697128837068475d, 0.8655365610061431d, 0.8613802928218509d}, new double[]{1.0d, 0.8572439828530728d, 0.7348672461377994d, 0.6299605249474366d, 0.540029869446153d, 0.46293735614364523d, 0.3968502629920499d, 0.3401975000435942d, 0.29163225989402913d, 0.25d, 0.2143109957132682d, 0.18371681153444985d, 0.15749013123685915d, 0.13500746736153826d, 0.11573433903591131d, 0.09921256574801247d, 0.08504937501089856d, 0.07290806497350728d, 0.0625d, 0.05357774892831705d, 0.04592920288361246d, 0.03937253280921479d, 0.033751866840384566d, 0.028933584758977827d, 0.02480314143700312d, 0.02126234375272464d, 0.01822701624337682d, 0.015625d, 0.013394437232079262d, 0.011482300720903116d, 0.009843133202303697d, 0.008437966710096141d}, new double[]{1.0d, 0.007233396189744457d, 5.232202043780962E-5d, 3.784659032745837E-7d, 2.7375938226945676E-9d, 1.9802100726146846E-11d, 1.4323643994144654E-13d, 1.03608591890502E-15d, 7.494419938055456E-18d, 5.421010862427522E-20d, 3.921231931684655E-22d, 2.8363824113752076E-24d, 2.0516677727099623E-26d, 1.4840525849741735E-28d, 1.0734740313532598E-30d, 7.764862968180291E-33d, 5.616633020792314E-35d, 4.062733189179202E-37d, 2.9387358770557188E-39d, 2.125704089576017E-41d, 1.537605986206337E-43d, 1.1122113281953186E-45d, 8.045065183558638E-48d, 5.819314384499884E-50d, 4.209340649576657E-52d, 3.0447828615984243E-54d, 2.2024120749685265E-56d, 1.5930919111324523E-58d, 1.1523464959898195E-60d, 8.335378753358135E-63d, 6.029309691461764E-65d, 4.3612385749008845E-67d}};
    static final double[][] EXPONENTIAL_DISTRIBUTION_P = {new double[]{1.0d, 0.9930924954370359d, 0.9862327044933592d, 0.9794202975869268d, 0.9726549474122855d, 0.9659363289248456d, 0.9592641193252643d, 0.9526379980439373d, 0.9460576467255959d, 0.9395227492140118d, 0.9330329915368074d, 0.9265880618903709d, 0.9201876506248751d, 0.9138314502294005d, 0.9075191553171609d, 0.9012504626108302d, 0.8950250709279725d, 0.8888426811665702d, 0.8827029962906549d, 0.8766057213160351d, 0.8705505632961241d, 0.8645372313078652d, 0.8585654364377537d, 0.8526348917679567d, 0.8467453123625271d, 0.8408964152537145d, 0.8350879194283694d, 0.8293195458144417d, 0.8235910172675731d, 0.8179020585577811d, 0.8122523963562355d, 0.8066417592221263d}, new double[]{1.0d, 0.8010698775896221d, 0.6417129487814521d, 0.5140569133280333d, 0.41179550863378656d, 0.32987697769322355d, 0.26425451014034507d, 0.2116863280906318d, 0.16957554093095897d, 0.13584185781575725d, 0.10881882041201552d, 0.08717147914690034d, 0.06983044612951375d, 0.05593906693299828d, 0.0448111015004946d, 0.03589682359365735d, 0.028755864082027346d, 0.023035456520173456d, 0.01845301033483641d, 0.014782150730087436d, 0.011841535675862484d, 0.009485897534336304d, 0.007598866776658481d, 0.0060872232785976555d, 0.004876291206646921d, 0.00390625d, 0.0031291792093344614d, 0.0025066912061775474d, 0.00200803481768763d, 0.0016085762056007287d, 0.0012885819441141545d, 0.001032244180235723d}, new double[]{1.0d, 8.268997191040304E-4d, 6.837631454543244E-7d, 5.654035529098692E-10d, 4.675320390815916E-13d, 3.866021117887027E-16d, 3.196811776431032E-19d, 2.643442759959277E-22d, 2.185862075677909E-25d, 1.807488736378216E-28d, 1.4946119283948456E-31d, 1.2358941837592312E-34d, 1.0219605533928131E-37d, 8.450588945359167E-41d, 6.987789625181121E-44d, 5.778201278220326E-47d, 4.777993013886938E-50d, 3.9509210810641284E-53d, 3.26701553213412E-56d, 2.701494225830208E-59d, 2.2338648165001596E-62d, 1.8471821892803583E-65d, 1.5274344334498962E-68d, 1.263035103969543E-71d, 1.044403372690945E-74d, 8.636168555094445E-78d, 7.1412453523426565E-81d, 5.905093775905105E-84d, 4.882920384578891E-87d, 4.037685494415629E-90d, 3.3387610011627014E-93d, 2.760820534016929E-96d}};

    Sample() {
    }

    private static int bernoulli(long j, long j2, double[][] dArr) {
        double d = 4.6116860184273879E18d;
        long j3 = 0;
        while (j3 < 3) {
            d *= dArr[(int) j3][(int) (31 & j2)];
            j3++;
            j2 >>= 5;
        }
        return (int) (((j & 4611686018427387903L) - round(d)) >>> 63);
    }

    public static void encodeC(int[] iArr, short[] sArr, byte[] bArr, int i, int i2, int i3) {
        short[] sArr2 = new short[i2];
        byte[] bArr2 = new byte[168];
        HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, 168, 0, bArr, i, 32);
        Arrays.fill(sArr2, (short) 0);
        short s = (short) 1;
        int i4 = 0;
        int i5 = 0;
        while (i5 < i3) {
            if (i4 > 165) {
                HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, 168, s, bArr, i, 32);
                i4 = 0;
                s = (short) (s + 1);
            }
            int i6 = ((bArr2[i4] << 8) | (bArr2[i4 + 1] & 255)) & (i2 - 1);
            if (sArr2[i6] == 0) {
                if ((bArr2[i4 + 2] & 1) == 1) {
                    sArr2[i6] = -1;
                } else {
                    sArr2[i6] = 1;
                }
                iArr[i5] = i6;
                sArr[i5] = sArr2[i6];
                i5++;
            }
            i4 += 3;
        }
    }

    private static long modulus7(long j) {
        for (int i = 0; i < 2; i++) {
            j = (j >> 3) + (7 & j);
        }
        return j & ((j - 7) >> 3);
    }

    public static void polynomialGaussSamplerI(int[] iArr, int i, byte[] bArr, int i2, int i3) {
        long modulus7;
        double d;
        long j;
        int i4;
        long j2;
        long j3;
        long j4;
        byte[] bArr2 = new byte[1024];
        short s = (short) (i3 << 8);
        HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, 1024, s, bArr, i2, 32);
        short s2 = (short) (s + 1);
        int i5 = 0;
        for (int i6 = 0; i6 < 512; i6++) {
            if (i5 + 46 > 512) {
                HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, 1024, s2, bArr, i2, 32);
                s2 = (short) (s2 + 1);
                i5 = 0;
            }
            while (true) {
                int i7 = i5 + 1;
                long load64 = CommonFunction.load64(bArr2, (i5 * 64) / 32);
                long j5 = 64;
                while (true) {
                    int i8 = i7 + 1;
                    long load642 = CommonFunction.load64(bArr2, (i7 * 64) / 32);
                    int i9 = i8 + 1;
                    long load643 = CommonFunction.load64(bArr2, (i8 * 64) / 32);
                    if (j5 <= 58) {
                        load64 = (load64 << 6) ^ ((load642 >>> 58) & 63);
                        j5 += 6;
                    }
                    long j6 = load642 & 288230376151711743L;
                    if (j6 <= 225463659665555712L) {
                        long j7 = 0;
                        int i10 = 0;
                        while (i10 < 12) {
                            long[][] jArr = CUMULATIVE_DISTRIBUTION_TABLE_I;
                            long j8 = load643 - jArr[i10][1];
                            j7 += (~((j6 - (jArr[i10][0] + (((((j8 & jArr[i10][1]) & 1) + (jArr[i10][1] >> 1)) + (j8 >>> 1)) >>> 63))) >>> 63)) & 1;
                            i10++;
                            j6 = j6;
                        }
                        while (true) {
                            if (j5 < 6) {
                                load64 = CommonFunction.load64(bArr2, (i9 * 64) / 32);
                                i9++;
                                j5 = 64;
                            }
                            long j9 = load64 & 63;
                            load64 >>= 6;
                            j5 -= 6;
                            if (j9 != 63) {
                                if (j5 < 2) {
                                    load64 = CommonFunction.load64(bArr2, (i9 * 64) / 32);
                                    i9++;
                                    j5 = 64;
                                }
                                modulus7 = (modulus7(j9) << 2) + (load64 & 3);
                                load64 >>= 2;
                                j5 -= 2;
                                d = (double) modulus7;
                                if (d < 27.0d) {
                                    break;
                                }
                            }
                        }
                        j = (long) ((((double) j7) * 27.0d) + d);
                        i4 = i9 + 1;
                        j2 = j << 1;
                        if (bernoulli(CommonFunction.load64(bArr2, (i9 * 64) / 32), modulus7 * (j2 - modulus7), EXPONENTIAL_DISTRIBUTION_I) != 0) {
                            break;
                        }
                        i7 = i4;
                    } else {
                        i7 = i9;
                    }
                }
                long j10 = 64;
                long j11 = load64 << ((int) (64 - j5));
                if (j5 == 0) {
                    j11 = CommonFunction.load64(bArr2, (i4 * 64) / 32);
                    i4++;
                } else {
                    j10 = j5;
                }
                long j12 = j11 >> 63;
                j3 = j11 << 1;
                j4 = j10 - 1;
                if (((j12 & 1) | j) != 0) {
                    break;
                }
                i5 = i4;
            }
            if (j4 == 0) {
                i5 = i4 + 1;
                j3 = CommonFunction.load64(bArr2, (i4 * 64) / 32);
            } else {
                i5 = i4;
            }
            iArr[i + i6] = (int) ((((j2 & (j3 >> 63)) - j) << 48) >> 48);
        }
    }

    public static void polynomialGaussSamplerIII(int[] iArr, int i, byte[] bArr, int i2, int i3, int i4, double d, double[][] dArr) {
        long modulus7;
        double d2;
        long j;
        int i5;
        long j2;
        long j3;
        long j4;
        int i6 = i4;
        int i7 = (i6 * 64) / 32;
        byte[] bArr2 = new byte[i7];
        short s = (short) (i3 << 8);
        HashUtils.customizableSecureHashAlgorithmKECCAK256Simple(bArr2, 0, i7, s, bArr, i2, 32);
        short s2 = (short) (s + 1);
        int i8 = 0;
        int i9 = 0;
        while (i9 < i6) {
            if (i8 + 46 > i6) {
                HashUtils.customizableSecureHashAlgorithmKECCAK256Simple(bArr2, 0, i7, s2, bArr, i2, 32);
                s2 = (short) (s2 + 1);
                i8 = 0;
            }
            while (true) {
                int i10 = i8 + 1;
                long load64 = CommonFunction.load64(bArr2, (i8 * 64) / 32);
                long j5 = 64;
                while (true) {
                    int i11 = i10 + 1;
                    long load642 = CommonFunction.load64(bArr2, (i10 * 64) / 32);
                    int i12 = i11 + 1;
                    long load643 = CommonFunction.load64(bArr2, (i11 * 64) / 32);
                    int i13 = i12 + 1;
                    long load644 = CommonFunction.load64(bArr2, (i12 * 64) / 32);
                    if (j5 <= 58) {
                        load64 = (load64 << 6) ^ ((load642 >>> 58) & 63);
                        j5 += 6;
                    }
                    long j6 = load642 & 4398046511103L;
                    if (j6 <= 3440302424096L) {
                        long j7 = 0;
                        int i14 = 0;
                        while (i14 < 14) {
                            long[][] jArr = CUMULATIVE_DISTRIBUTION_TABLE_III;
                            long j8 = load644 - jArr[i14][2];
                            long j9 = ((((j8 & jArr[i14][2]) & 1) + (jArr[i14][2] >> 1)) + (j8 >>> 1)) >> 63;
                            long j10 = load643 - (jArr[i14][1] + j9);
                            j7 += (~((j6 - (jArr[i14][0] + (((((j10 & j9) & 1) + (jArr[i14][1] >> 1)) + (j10 >>> 1)) >> 63))) >>> 63)) & 1;
                            i14++;
                            i7 = i7;
                        }
                        while (true) {
                            if (j5 < 6) {
                                load64 = CommonFunction.load64(bArr2, (i13 * 64) / 32);
                                i13++;
                                j5 = 64;
                            }
                            long j11 = load64 & 63;
                            load64 >>= 6;
                            j5 -= 6;
                            if (j11 != 63) {
                                if (j5 < 2) {
                                    load64 = CommonFunction.load64(bArr2, (i13 * 64) / 32);
                                    i13++;
                                    j5 = 64;
                                }
                                modulus7 = (modulus7(j11) << 2) + (load64 & 3);
                                load64 >>= 2;
                                j5 -= 2;
                                d2 = (double) modulus7;
                                if (d2 < d) {
                                    break;
                                }
                            }
                        }
                        j = (long) ((((double) j7) * d) + d2);
                        i5 = i13 + 1;
                        j2 = j << 1;
                        if (bernoulli(CommonFunction.load64(bArr2, (i13 * 64) / 32), modulus7 * (j2 - modulus7), dArr) != 0) {
                            break;
                        }
                        i10 = i5;
                        i7 = i7;
                    } else {
                        i10 = i13;
                    }
                }
                long j12 = 64;
                long j13 = load64 << ((int) (64 - j5));
                if (j5 == 0) {
                    j13 = CommonFunction.load64(bArr2, (i5 * 64) / 32);
                    i5++;
                } else {
                    j12 = j5;
                }
                long j14 = j13 >> 63;
                j3 = j13 << 1;
                j4 = j12 - 1;
                if (((j14 & 1) | j) != 0) {
                    break;
                }
                i8 = i5;
                i7 = i7;
            }
            if (j4 == 0) {
                j3 = CommonFunction.load64(bArr2, (i5 * 64) / 32);
                i5++;
            }
            iArr[i + i9] = (int) ((((j2 & (j3 >> 63)) - j) << 48) >> 48);
            i9++;
            i6 = i4;
            i8 = i5;
            i7 = i7;
        }
    }

    public static void polynomialGaussSamplerIIIP(long[] jArr, int i, byte[] bArr, int i2, int i3) {
        long modulus7;
        double d;
        long j;
        int i4;
        long j2;
        long j3;
        long j4;
        long j5;
        byte[] bArr2 = new byte[PKIFailureInfo.certConfirmed];
        short s = (short) (i3 << 8);
        HashUtils.customizableSecureHashAlgorithmKECCAK256Simple(bArr2, 0, PKIFailureInfo.certConfirmed, s, bArr, i2, 32);
        short s2 = (short) (s + 1);
        int i5 = 0;
        for (int i6 = 0; i6 < 2048; i6++) {
            if (i5 + 46 > 2048) {
                HashUtils.customizableSecureHashAlgorithmKECCAK256Simple(bArr2, 0, PKIFailureInfo.certConfirmed, s2, bArr, i2, 32);
                s2 = (short) (s2 + 1);
                i5 = 0;
            }
            do {
                int i7 = i5 + 1;
                long load64 = CommonFunction.load64(bArr2, (i5 * 64) / 32);
                long j6 = 64;
                while (true) {
                    int i8 = i7 + 1;
                    long load642 = CommonFunction.load64(bArr2, (i7 * 64) / 32);
                    int i9 = i8 + 1;
                    long load643 = CommonFunction.load64(bArr2, (i8 * 64) / 32);
                    int i10 = i9 + 1;
                    long load644 = CommonFunction.load64(bArr2, (i9 * 64) / 32);
                    if (j6 <= 58) {
                        load64 = (load64 << 6) ^ ((load642 >>> 58) & 63);
                        j6 += 6;
                    }
                    long j7 = load642 & 4398046511103L;
                    if (j7 <= 3440302424096L) {
                        long j8 = 0;
                        int i11 = 0;
                        while (i11 < 14) {
                            long[][] jArr2 = CUMULATIVE_DISTRIBUTION_TABLE_III;
                            long j9 = load644 - jArr2[i11][2];
                            long j10 = ((((j9 & jArr2[i11][2]) & 1) + (jArr2[i11][2] >> 1)) + (j9 >>> 1)) >> 63;
                            long j11 = load643 - (jArr2[i11][1] + j10);
                            j8 += (~((j7 - (jArr2[i11][0] + (((((j11 & j10) & 1) + (jArr2[i11][1] >> 1)) + (j11 >>> 1)) >> 63))) >>> 63)) & 1;
                            i11++;
                            i10 = i10;
                            j7 = j7;
                        }
                        while (true) {
                            if (j6 < 6) {
                                load64 = CommonFunction.load64(bArr2, (i10 * 64) / 32);
                                i10++;
                                j6 = 64;
                            }
                            long j12 = load64 & 63;
                            load64 >>= 6;
                            j6 -= 6;
                            if (j12 != 63) {
                                if (j6 < 2) {
                                    load64 = CommonFunction.load64(bArr2, (i10 * 64) / 32);
                                    i10++;
                                    j6 = 64;
                                }
                                modulus7 = (modulus7(j12) << 2) + (load64 & 3);
                                load64 >>= 2;
                                j6 -= 2;
                                d = (double) modulus7;
                                if (d < 10.0d) {
                                    break;
                                }
                            }
                        }
                        j = (long) ((((double) j8) * 10.0d) + d);
                        i4 = i10 + 1;
                        j2 = j << 1;
                        if (bernoulli(CommonFunction.load64(bArr2, (i10 * 64) / 32), modulus7 * (j2 - modulus7), EXPONENTIAL_DISTRIBUTION_P) != 0) {
                            break;
                        }
                        i7 = i4;
                    } else {
                        i7 = i10;
                    }
                }
                long j13 = 64;
                long j14 = load64 << ((int) (64 - j6));
                if (j6 == 0) {
                    i5 = i4 + 1;
                    j14 = CommonFunction.load64(bArr2, (i4 * 64) / 32);
                } else {
                    j13 = j6;
                    i5 = i4;
                }
                j3 = j14 >> 63;
                j4 = j14 << 1;
                j5 = j13 - 1;
            } while (((j3 & 1) | j) == 0);
            if (j5 == 0) {
                j4 = CommonFunction.load64(bArr2, (i5 * 64) / 32);
                i5++;
            }
            jArr[i + i6] = (((j2 & (j4 >> 63)) - j) << 48) >> 48;
        }
    }

    public static void polynomialGaussSamplerIP(long[] jArr, int i, byte[] bArr, int i2, int i3) {
        long modulus7;
        double d;
        long j;
        int i4;
        long j2;
        long j3;
        long j4;
        byte[] bArr2 = new byte[2048];
        short s = (short) (i3 << 8);
        HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, 2048, s, bArr, i2, 32);
        short s2 = (short) (s + 1);
        int i5 = 0;
        for (int i6 = 0; i6 < 1024; i6++) {
            if (i5 + 46 > 1024) {
                HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, 2048, s2, bArr, i2, 32);
                s2 = (short) (s2 + 1);
                i5 = 0;
            }
            while (true) {
                int i7 = i5 + 1;
                long load64 = CommonFunction.load64(bArr2, (i5 * 64) / 32);
                long j5 = 64;
                while (true) {
                    int i8 = i7 + 1;
                    long load642 = CommonFunction.load64(bArr2, (i7 * 64) / 32);
                    int i9 = i8 + 1;
                    long load643 = CommonFunction.load64(bArr2, (i8 * 64) / 32);
                    if (j5 <= 58) {
                        load64 = (load64 << 6) ^ ((load642 >>> 58) & 63);
                        j5 += 6;
                    }
                    long j6 = load642 & 288230376151711743L;
                    if (j6 <= 225463659665555712L) {
                        long j7 = 0;
                        int i10 = 0;
                        while (i10 < 12) {
                            long[][] jArr2 = CUMULATIVE_DISTRIBUTION_TABLE_I;
                            long j8 = load643 - jArr2[i10][1];
                            j7 += (~((j6 - (jArr2[i10][0] + (((((j8 & jArr2[i10][1]) & 1) + (jArr2[i10][1] >> 1)) + (j8 >>> 1)) >>> 63))) >>> 63)) & 1;
                            i10++;
                            j6 = j6;
                        }
                        while (true) {
                            if (j5 < 6) {
                                load64 = CommonFunction.load64(bArr2, (i9 * 64) / 32);
                                i9++;
                                j5 = 64;
                            }
                            long j9 = load64 & 63;
                            load64 >>= 6;
                            j5 -= 6;
                            if (j9 != 63) {
                                if (j5 < 2) {
                                    load64 = CommonFunction.load64(bArr2, (i9 * 64) / 32);
                                    i9++;
                                    j5 = 64;
                                }
                                modulus7 = (modulus7(j9) << 2) + (load64 & 3);
                                load64 >>= 2;
                                j5 -= 2;
                                d = (double) modulus7;
                                if (d < 10.0d) {
                                    break;
                                }
                            }
                        }
                        j = (long) ((((double) j7) * 10.0d) + d);
                        i4 = i9 + 1;
                        j2 = j << 1;
                        if (bernoulli(CommonFunction.load64(bArr2, (i9 * 64) / 32), modulus7 * (j2 - modulus7), EXPONENTIAL_DISTRIBUTION_P) != 0) {
                            break;
                        }
                        i7 = i4;
                    } else {
                        i7 = i9;
                    }
                }
                long j10 = 64;
                long j11 = load64 << ((int) (64 - j5));
                if (j5 == 0) {
                    j11 = CommonFunction.load64(bArr2, (i4 * 64) / 32);
                    i4++;
                } else {
                    j10 = j5;
                }
                long j12 = j11 >> 63;
                j3 = j11 << 1;
                j4 = j10 - 1;
                if (((j12 & 1) | j) != 0) {
                    break;
                }
                i5 = i4;
            }
            if (j4 == 0) {
                i5 = i4 + 1;
                j3 = CommonFunction.load64(bArr2, (i4 * 64) / 32);
            } else {
                i5 = i4;
            }
            jArr[i + i6] = (((j2 & (j3 >> 63)) - j) << 48) >> 48;
        }
    }

    private static long round(double d) {
        return (long) (d < 0.0d ? d - 0.5d : d + 0.5d);
    }

    public static void sampleY(int[] iArr, byte[] bArr, int i, int i2, int i3, int i4, int i5, int i6) {
        int i7;
        int i8;
        int i9 = i6 + 1;
        int i10 = (i9 + 7) / 8;
        int i11 = i3 * i10;
        byte[] bArr2 = new byte[i11];
        int i12 = 4;
        int[] iArr2 = new int[4];
        short s = (short) (i2 << 8);
        if (i4 == 4205569) {
            HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, i11, s, bArr, i, 32);
            s = (short) (s + 1);
        }
        if (i4 == 4206593 || i4 == 8404993) {
            HashUtils.customizableSecureHashAlgorithmKECCAK256Simple(bArr2, 0, i11, s, bArr, i, 32);
            i7 = i3;
            s = (short) (s + 1);
        } else {
            i7 = i3;
        }
        int i13 = 0;
        int i14 = 0;
        while (i14 < i3) {
            if (i13 > i7 * i10 * i12) {
                if (i4 == 4205569) {
                    i8 = 168 / i10;
                    HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, 168, s, bArr, i, 32);
                    s = (short) (s + 1);
                } else {
                    i8 = i7;
                }
                if (i4 == 4206593 || i4 == 8404993) {
                    i8 = 136 / i10;
                    HashUtils.customizableSecureHashAlgorithmKECCAK256Simple(bArr2, 0, 136, s, bArr, i, 32);
                    s = (short) (s + 1);
                }
                i13 = 0;
                i7 = i8;
            }
            int i15 = (1 << i9) - 1;
            iArr2[0] = (CommonFunction.load32(bArr2, i13) & i15) - i5;
            iArr2[1] = (CommonFunction.load32(bArr2, i13 + i10) & i15) - i5;
            iArr2[2] = (CommonFunction.load32(bArr2, (i10 * 2) + i13) & i15) - i5;
            iArr2[3] = (CommonFunction.load32(bArr2, (i10 * 3) + i13) & i15) - i5;
            if (i14 < i3 && iArr2[0] != (1 << i6)) {
                iArr[i14] = iArr2[0];
                i14++;
            }
            if (i14 < i3 && iArr2[1] != (1 << i6)) {
                iArr[i14] = iArr2[1];
                i14++;
            }
            if (i14 < i3 && iArr2[2] != (1 << i6)) {
                iArr[i14] = iArr2[2];
                i14++;
            }
            if (i14 < i3 && iArr2[3] != (1 << i6)) {
                iArr[i14] = iArr2[3];
                i14++;
            }
            i13 += i10 * 4;
            i12 = 4;
        }
    }

    public static void sampleY(long[] jArr, byte[] bArr, int i, int i2, int i3, int i4, int i5, int i6) {
        int i7;
        int i8;
        int i9 = i6 + 1;
        int i10 = (i9 + 7) / 8;
        int i11 = i3 * i10;
        byte[] bArr2 = new byte[i11];
        short s = (short) (i2 << 8);
        int i12 = Parameter.Q_I_P;
        if (i4 == 485978113) {
            HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, i11, s, bArr, i, 32);
            s = (short) (s + 1);
        }
        int i13 = Parameter.Q_III_P;
        if (i4 == 1129725953) {
            HashUtils.customizableSecureHashAlgorithmKECCAK256Simple(bArr2, 0, i11, s, bArr, i, 32);
            i7 = i3;
            s = (short) (s + 1);
        } else {
            i7 = i3;
        }
        int i14 = 0;
        int i15 = 0;
        while (i15 < i3) {
            if (i14 > i7 * i10) {
                if (i4 == i12) {
                    i8 = 168 / i10;
                    HashUtils.customizableSecureHashAlgorithmKECCAK128Simple(bArr2, 0, 168, s, bArr, i, 32);
                    s = (short) (s + 1);
                } else {
                    i8 = i7;
                }
                if (i4 == i13) {
                    i8 = 136 / i10;
                    HashUtils.customizableSecureHashAlgorithmKECCAK256Simple(bArr2, 0, 136, s, bArr, i, 32);
                    s = (short) (s + 1);
                }
                i14 = 0;
                i7 = i8;
            }
            jArr[i15] = (long) ((CommonFunction.load32(bArr2, i14) & ((1 << i9) - 1)) - i5);
            if (jArr[i15] != ((long) (1 << i6))) {
                i15++;
            }
            i14 += i10;
            i12 = Parameter.Q_I_P;
            i13 = Parameter.Q_III_P;
        }
    }
}
