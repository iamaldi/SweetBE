# SweetBE

Let's say you are testing a web application and you're manually sending requests to identify IDOR, XSS or SQLi vulnerabilities (to name a few).

With SweetBE you can set clear criteria to replace specific parameters/values. For example, if your current user has access to resource X but it is not supposed to access resource Y, you can create a find & replace criteria to inject the resource Y parameter in each request where resource X identifier is present. In SweetBE this IDOR vulnerability could be tested using the following criteria entry:

```json
[
    {
        "find_what": "X",
        "replace_with": "Y",
        "success_string": "200 OK"
    }
]
```

If SweetBE detects an HTTP request with the "X" parameter/value, it is going to send an additional request by replacing X with Y and will mark the request/response attack as successful if the response contains the "200 OK" string.