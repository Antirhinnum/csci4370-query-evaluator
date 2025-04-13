package uga.cs4370.mydbfrontend;

public final class NameableImpl<T> implements Nameable<T> {

    private T value;
    private String name;

    public NameableImpl(T value, String name) {
        setValue(value);
        setName(name);
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
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) throws NullPointerException {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
    }

}
