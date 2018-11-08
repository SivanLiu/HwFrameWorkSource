package tmsdk.fg.module.cleanV2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RubbishHolder {
    Map<String, RubbishEntity> NT;
    List<RubbishEntity> NU;
    Map<String, RubbishEntity> NV;
    Map<String, RubbishEntity> NW;

    private void a(RubbishEntity rubbishEntity) {
        if (this.NT == null) {
            this.NT = new HashMap();
        }
        int -l_2_I = 0;
        RubbishEntity -l_3_R = (RubbishEntity) this.NT.get(rubbishEntity.getDescription());
        if (-l_3_R != null) {
            -l_3_R.a(rubbishEntity.getRubbishKey(), rubbishEntity.getSize());
            -l_2_I = 1;
        }
        if (-l_2_I == 0) {
            this.NT.put(rubbishEntity.getDescription(), rubbishEntity);
        }
    }

    private void b(RubbishEntity rubbishEntity) {
        if (this.NU == null) {
            this.NU = new ArrayList();
        }
        this.NU.add(rubbishEntity);
    }

    private void c(RubbishEntity rubbishEntity) {
        if (this.NV == null) {
            this.NV = new HashMap();
        }
        Object -l_2_R = rubbishEntity.getPackageName() + rubbishEntity.getDescription();
        RubbishEntity -l_3_R = (RubbishEntity) this.NV.get(-l_2_R);
        if (-l_3_R != null) {
            -l_3_R.a(rubbishEntity.getRubbishKey(), rubbishEntity.getSize());
        } else {
            this.NV.put(-l_2_R, rubbishEntity);
        }
    }

    private void d(RubbishEntity rubbishEntity) {
        if (this.NW == null) {
            this.NW = new HashMap();
        }
        Object -l_2_R = rubbishEntity.getPackageName() + rubbishEntity.getDescription();
        RubbishEntity -l_3_R = (RubbishEntity) this.NW.get(-l_2_R);
        if (-l_3_R != null) {
            -l_3_R.a(rubbishEntity.getRubbishKey(), rubbishEntity.getSize());
        } else {
            this.NW.put(-l_2_R, rubbishEntity);
        }
    }

    public void addRubbish(RubbishEntity rubbishEntity) {
        switch (rubbishEntity.getRubbishType()) {
            case 0:
                c(rubbishEntity);
                return;
            case 1:
                a(rubbishEntity);
                return;
            case 2:
                b(rubbishEntity);
                return;
            case 4:
                d(rubbishEntity);
                return;
            default:
                return;
        }
    }

    public long getAllRubbishFileSize() {
        long -l_1_J = 0;
        if (this.NT != null) {
            for (RubbishEntity -l_4_R : this.NT.values()) {
                -l_1_J += -l_4_R.getSize();
            }
        }
        if (this.NU != null) {
            for (RubbishEntity -l_4_R2 : this.NU) {
                -l_1_J += -l_4_R2.getSize();
            }
        }
        if (this.NV != null) {
            for (RubbishEntity -l_4_R22 : this.NV.values()) {
                -l_1_J += -l_4_R22.getSize();
            }
        }
        if (this.NW != null) {
            for (RubbishEntity -l_4_R222 : this.NW.values()) {
                -l_1_J += -l_4_R222.getSize();
            }
        }
        return -l_1_J;
    }

    public long getCleanRubbishFileSize() {
        long -l_1_J = 0;
        if (this.NT != null) {
            for (RubbishEntity -l_4_R : this.NT.values()) {
                if (2 == -l_4_R.getStatus()) {
                    -l_1_J += -l_4_R.getSize();
                }
            }
        }
        if (this.NU != null) {
            for (RubbishEntity -l_4_R2 : this.NU) {
                if (2 == -l_4_R2.getStatus()) {
                    -l_1_J += -l_4_R2.getSize();
                }
            }
        }
        if (this.NV != null) {
            for (RubbishEntity -l_4_R22 : this.NV.values()) {
                if (2 == -l_4_R22.getStatus()) {
                    -l_1_J += -l_4_R22.getSize();
                }
            }
        }
        if (this.NW != null) {
            for (RubbishEntity -l_4_R222 : this.NW.values()) {
                if (2 == -l_4_R222.getStatus()) {
                    -l_1_J += -l_4_R222.getSize();
                }
            }
        }
        return -l_1_J;
    }

    public long getSelectedRubbishFileSize() {
        long -l_1_J = 0;
        if (this.NT != null) {
            for (RubbishEntity -l_4_R : this.NT.values()) {
                if (1 == -l_4_R.getStatus()) {
                    -l_1_J += -l_4_R.getSize();
                }
            }
        }
        if (this.NU != null) {
            for (RubbishEntity -l_4_R2 : this.NU) {
                if (1 == -l_4_R2.getStatus()) {
                    -l_1_J += -l_4_R2.getSize();
                }
            }
        }
        if (this.NV != null) {
            for (RubbishEntity -l_4_R22 : this.NV.values()) {
                if (1 == -l_4_R22.getStatus()) {
                    -l_1_J += -l_4_R22.getSize();
                }
            }
        }
        if (this.NW != null) {
            for (RubbishEntity -l_4_R222 : this.NW.values()) {
                if (1 == -l_4_R222.getStatus()) {
                    -l_1_J += -l_4_R222.getSize();
                }
            }
        }
        return -l_1_J;
    }

    public long getSuggetRubbishFileSize() {
        long -l_1_J = 0;
        if (this.NT != null) {
            for (RubbishEntity -l_4_R : this.NT.values()) {
                if (-l_4_R.isSuggest()) {
                    -l_1_J += -l_4_R.getSize();
                }
            }
        }
        if (this.NU != null) {
            for (RubbishEntity -l_4_R2 : this.NU) {
                if (-l_4_R2.isSuggest()) {
                    -l_1_J += -l_4_R2.getSize();
                }
            }
        }
        if (this.NV != null) {
            for (RubbishEntity -l_4_R22 : this.NV.values()) {
                if (-l_4_R22.isSuggest()) {
                    -l_1_J += -l_4_R22.getSize();
                }
            }
        }
        if (this.NW != null) {
            for (RubbishEntity -l_4_R222 : this.NW.values()) {
                if (-l_4_R222.isSuggest()) {
                    -l_1_J += -l_4_R222.getSize();
                }
            }
        }
        return -l_1_J;
    }

    public List<RubbishEntity> getmApkRubbishes() {
        return this.NU;
    }

    public Map<String, RubbishEntity> getmInstallRubbishes() {
        return this.NV;
    }

    public Map<String, RubbishEntity> getmSystemRubbishes() {
        return this.NT;
    }

    public Map<String, RubbishEntity> getmUnInstallRubbishes() {
        return this.NW;
    }

    public void resetRubbishes() {
        this.NT.clear();
        this.NU.clear();
        this.NV.clear();
        this.NW.clear();
    }
}
