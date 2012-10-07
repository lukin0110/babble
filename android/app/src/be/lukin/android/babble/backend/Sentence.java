package be.lukin.android.babble.backend;

public class Sentence implements BabblePhrase {
	private int id;
	private String locale;
	private String value;
	private int group;

	// Sentence with the default ID and group ID.
	// Note: the IDs are not unique ;)
	public Sentence(String value, String locale) {
		this(1, locale, value, 2);
	}

	public Sentence(int id, String locale, String value, int group) {
		this.id = id;
		this.locale = locale;
		this.value = value;
		this.group = group;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getGroup() {
		return group;
	}

	public void setGroup(int group) {
		this.group = group;
	}

	@Override
	public String toString() {
		return "Sentence{" +
				"id=" + id +
				", locale='" + locale + '\'' +
				", value='" + value + '\'' +
				", group=" + group +
				'}';
	}
}
