package org.nmcpye.datarun.etl.util.jsonplay;

public class JsonSamples {
    public static final String SAMPLE_SIMPLE = """
    {
      "id": 101,
      "name": "Alice",
      "age": 30,
      "active": true
    }
    """;

    public static final String SAMPLE_NESTED = """
    {
      "user": {
        "id": 42,
        "profile": {
          "email": "user@example.com",
          "address": {
            "street": "Main St",
            "city": "Aden",
            "zip": "12345"
          }
        }
      },
      "roles": ["admin", "editor"]
    }
    """;

    public static final String SAMPLE_COMPLEX = """
    {
      "orderId": "ORD-555",
      "total": 129.99,
      "items": [
        {
          "sku": "A100",
          "qty": 2,
          "price": 19.99
        },
        {
          "sku": "B200",
          "qty": 1,
          "price": 89.99,
          "discounts": [5, 10]
        }
      ],
      "customer": {
        "name": "Hamza",
        "phones": [
          {"type": "mobile", "number": "+967700000000"},
          {"type": "home", "number": null}
        ]
      }
    }
    """;
}
