package uga.cs4370.mydbfrontend;

import java.util.Optional;

public final class AliasableImpl<T> implements Aliasable<T> {

    private T value;
    private String name;
    private String alias;

    public AliasableImpl(T value, String name) {
        this(value, name, null);
    }

    public AliasableImpl(T value, String name, String alias) {
        setValue(value);
        setName(name);
        setAlias(alias);
    }

    @Override
    public Optional<String> getAlias() {
        return Optional.of(this.alias);
    }

    @Override
    public void setAlias(String value) {
        this.alias = value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String value) {
        if (value == null) {
            throw new NullPointerException();
        }
        this.name = value;
    }

    @Override
    public T getValue() {
        return this.value;
    }

    @Override
    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String getNameOrAlias() {
        return (this.alias != null) ? this.alias : this.name;
    }
}
