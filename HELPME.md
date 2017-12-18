#### An Example Table

| firstName | lastName | email | phone | jobtitle | dept | password | groups |
| --------- | -------- | ----- | ----- | -------- | ---- | -------- | ------ |
| Some | Name | some.name@example.com | ~ | Some Specialist | Example Creation | ************ | group1@example.com group2@example.com group3@example.com |

Commands are run in this order:  
    1. help (will exit) / reset (will not exit)  
    2. create user  
    3. update user details  
    4. add member to group  

You probably won't need to use reset, but it could be useful in some situations.
It is highly recommended to run with verbose output since errors will not be
logged otherwise, and may change when rerun.
