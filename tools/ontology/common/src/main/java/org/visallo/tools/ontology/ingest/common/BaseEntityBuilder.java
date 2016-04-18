package org.visallo.tools.ontology.ingest.common;

import com.google.common.base.Objects;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class BaseEntityBuilder {
    private String id;
    private String visibility;
    private Long timestamp;
    private Set<PropertyAddition<?>> propertyAdditions = new HashSet<>();

    public BaseEntityBuilder(String id) {
        assert id != null;
        assert id.trim().length() > 0;

        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    public abstract String getIri();

    public Set<PropertyAddition<?>> getPropertyAdditions() {
        return propertyAdditions;
    }

    protected PropertyAddition<String> addStringProperty(String iri, String key, Object value) {
        String strValue = null;
        if (value != null) {
            strValue = value.toString();
            strValue = strValue.trim().length() > 0 ? strValue : null;
        }
        return addTo(iri, key, strValue);
    }

    protected PropertyAddition<Date> addDateProperty(String iri, String key, Object value, SimpleDateFormat dateFormat) {
        Date dateValue = null;
        if (value != null) {
            if (value instanceof Date) {
                dateValue = (Date) value;
            } else {
                String strValue = value.toString();
                try {
                    dateValue = strValue.trim().length() > 0 ? dateFormat.parse(strValue) : null;
                } catch (ParseException pe) {
                    throw new VisalloException("Unable to parse date: " + strValue, pe);
                }
            }
        }
        return addTo(iri, key, dateValue);
    }

    protected PropertyAddition<byte[]> addByteArrayProperty(String iri, String key, Object value) {
        byte[] byteArrayValue = null;
        if (value != null) {
            if (value instanceof byte[]) {
                byteArrayValue = (byte[]) value;
            } else {
                throw new VisalloException("Unable to assign value " + value + " as byte[]");
            }
        }
        return addTo(iri, key, byteArrayValue);
    }

    protected PropertyAddition<Boolean> addBooleanProperty(String iri, String key, Object value) {
        Boolean booleanValue = null;
        if (value != null) {
            if (value instanceof Boolean) {
                booleanValue = (Boolean) value;
            } else {
                booleanValue = Boolean.valueOf(value.toString());
            }
        }
        return addTo(iri, key, booleanValue);
    }

    protected PropertyAddition<Double> addDoubleProperty(String iri, String key, Object value) {
        Double doubleValue = null;
        if (value != null) {
            if (value instanceof String) {
                doubleValue = Double.valueOf((String) value);
            } else if (value instanceof Integer) {
                doubleValue = ((Integer) value).doubleValue();
            } else {
                doubleValue = (Double) value;
            }
        }
        return addTo(iri, key, doubleValue);
    }

    protected PropertyAddition<Integer> addIntegerProperty(String iri, String key, Object value) {
        Integer intValue = null;
        if (value != null) {
            if (value instanceof String) {
                intValue = Integer.valueOf((String) value);
            } else if (value instanceof Double) {
                intValue = ((Double) value).intValue();
            } else {
                intValue = (Integer) value;
            }
        }
        return addTo(iri, key, intValue);
    }

    protected PropertyAddition<Long> addLongProperty(String iri, String key, Object value) {
        Long longValue = null;
        if (value != null) {
            if (value instanceof String) {
                longValue = Long.valueOf((String) value);
            } else if (value instanceof Integer) {
                longValue = ((Integer) value).longValue();
            } else if (value instanceof Double) {
                longValue = ((Double) value).longValue();
            } else {
                longValue = (Long) value;
            }
        }
        return addTo(iri, key, longValue);
    }

    protected PropertyAddition<GeoPoint> addGeoPointProperty(String iri, String key, Object value) {
        GeoPoint geoValue = null;
        if (value != null) {
            geoValue = (GeoPoint) value;
        }
        return addTo(iri, key, geoValue);
    }

    private <T> PropertyAddition<T> addTo(String iri, String key, T value) {
        PropertyAddition<T> addition = new PropertyAddition<>(iri, key, value);
        propertyAdditions.add(addition);
        return addition;
    }

    public class PropertyAddition<T> {
        private String iri;
        private T value;
        private String key;
        private Map<String, Object> metadata;
        private Long timestamp;
        private String visibility;

        public PropertyAddition(String iri, String key, T value) {
            assert iri != null;
            assert iri.trim().length() > 0;
            assert key != null;
            assert key.trim().length() > 0;

            this.iri = iri;
            this.key = key;
            this.value = value;
        }

        public PropertyAddition withMetadata(Map<String, Object> metdata) {
            this.metadata = metdata;
            return this;
        }

        public PropertyAddition withTimestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public PropertyAddition withVisibility(String visibility) {
            this.visibility = visibility;
            return this;
        }

        public String getIri() {
            return iri;
        }

        public T getValue() {
            return value;
        }

        public String getKey() {
            return key;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public String getVisibility() {
            return visibility;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return iri.equals(((PropertyAddition<?>) o).iri) && key.equals(((PropertyAddition<?>) o).key);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(iri, key);
        }
    }
}
