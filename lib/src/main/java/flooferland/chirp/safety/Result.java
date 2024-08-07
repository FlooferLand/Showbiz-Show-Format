package flooferland.chirp.safety;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Result<TOk, TErr> {
    private final boolean hasValue;
    private final @Nullable TOk value;
    private final @Nullable TErr error;
    
    public boolean hasValue() {
        return this.hasValue && this.value != null;
    }
    
    private Result(@Nullable TOk value, @Nullable TErr error) {
        this.value = value;
        this.error = error;
        this.hasValue = value != null;
    }
    
    // region | Constructors
    /** Stores a value; stores nothing is the value is <c>null</c> */
    public static <TOk, TErr> Result<TOk, TErr> ok(@Nonnull TOk value) {
        return new Result<>(value, null);
    }
    
    public static <TOk, TErr> Result<TOk, TErr> err(@Nonnull TErr error) {
        return new Result<>(null, error);
    }
    public static <TOk> Result<TOk, String> err(@Nonnull String formatErr, Object ... args) {
        return new Result<>(null, String.format(formatErr, args));
    }

    /** Stores a value if the condition is true, otherwise it stores an error */
    public static <TOk, TErr> Result<TOk, TErr> conditional(boolean condition, @Nonnull TOk value, @Nonnull TErr error) {
        return condition ? ok(value) : err(error);
    }
    // endregion
    
    // region | Getting the value
    public @Nullable TOk letOk() {
        return this.value;
    }
    public @Nullable TErr letErr() {
        return this.error;
    }

    public TOk unwrap() {
        if (hasValue())
            return value;
        else
            throw new RuntimeException(String.valueOf(error));
    }
    
    public TOk unwrapOr(TOk _default) {
        if (value != null)
            return value;
        else
            return _default;
    }

    public TOk expect(String message) {
        if (hasValue())
            return value;
        else
            throw new RuntimeException(message);
    }
    // endregion

    // region | Value mapping
    public interface IMapSome<TInput, TOutput> { TOutput mapper(TInput value); }

    /** If there is a value, it'll map the value through this function */
    public <TOutputOk> Result<TOutputOk, TErr> mapOk(IMapSome<TOk, TOutputOk> map) {
        if (hasValue())
            return Result.ok(map.mapper(value));
        else
            return err(error);
    }
    
    /** If there is an error, it'll map the error through this function */
    public <TOutputErr> Result<TOk, TOutputErr> mapErr(IMapSome<TErr, TOutputErr> map) {
        if (hasValue())
            return Result.ok(value);
        else
            return err(map.mapper(error));
    }
    // endregion
}
