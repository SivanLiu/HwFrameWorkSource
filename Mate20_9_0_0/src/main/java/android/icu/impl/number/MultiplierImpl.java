package android.icu.impl.number;

import java.math.BigDecimal;

public class MultiplierImpl implements MicroPropsGenerator {
    final BigDecimal bigDecimalMultiplier;
    final int magnitudeMultiplier;
    final MicroPropsGenerator parent;

    public MultiplierImpl(int magnitudeMultiplier) {
        this.magnitudeMultiplier = magnitudeMultiplier;
        this.bigDecimalMultiplier = null;
        this.parent = null;
    }

    public MultiplierImpl(BigDecimal bigDecimalMultiplier) {
        this.magnitudeMultiplier = 0;
        this.bigDecimalMultiplier = bigDecimalMultiplier;
        this.parent = null;
    }

    private MultiplierImpl(MultiplierImpl base, MicroPropsGenerator parent) {
        this.magnitudeMultiplier = base.magnitudeMultiplier;
        this.bigDecimalMultiplier = base.bigDecimalMultiplier;
        this.parent = parent;
    }

    public MicroPropsGenerator copyAndChain(MicroPropsGenerator parent) {
        return new MultiplierImpl(this, parent);
    }

    public MicroProps processQuantity(DecimalQuantity quantity) {
        MicroProps micros = this.parent.processQuantity(quantity);
        quantity.adjustMagnitude(this.magnitudeMultiplier);
        if (this.bigDecimalMultiplier != null) {
            quantity.multiplyBy(this.bigDecimalMultiplier);
        }
        return micros;
    }
}
