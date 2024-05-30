<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head lang="en">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"></meta>
    <title>菜单</title>
    <style>
        html, body {
            height: 100%;
            margin: 0;
            padding: 0;
        }
        .container {
            background: rgba(0, 0, 0, 0.2) url('${bg}') center center no-repeat; background-size: cover;
            width: ${width?replace(",", "")}px;
            height: ${height?replace(",", "")}px;
            padding: 10px;
        }
        table {
            width: ${width?replace(",", "")}px;
            table-layout: fixed;
            border-collapse: collapse;
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
            background: linear-gradient(45deg, #a37d7d, #13bfb7);
            opacity: 0.88;
        }
        th {
            text-align: center;
            font-weight: bold;
            padding: 5px;
            border: 1px solid black;
            font-size: 16px;
        }
        tr:nth-child(even) {
            background-color: #f2f2f2;
            font-family: sans-serif;
        }
        td {
            padding: 5px;
            border: 1px solid black;
            list-style: disc inside;
            min-width: 60px;
            font-size: 14px;
        }
        tr {
            height: 32px;
        }
    </style>
</head>
<body>
<div class="container">
    <table>
        <caption style="font-size: 24px; height: 40px; line-height: 40px;">BOT菜单指令</caption>
        <tr>
            <th colspan="1">序号</th>
            <th colspan="2">指令</th>
            <th colspan="1">授权</th>
            <th colspan="3">描述</th>
        </tr>
        <#list list as item>
            <tr>
                <td colspan="1" style="text-align: center">${item_index + 1}</td>
                <td colspan="2" style="white-space: nowrap; text-overflow:ellipsis; overflow: hidden;">${item.cmd}</td>
                <#if item.permit == true>
                    <td colspan="1">是</td>
                <#else >
                    <td colspan="1">否</td>
                </#if>
                <td colspan="3" style="white-space: nowrap; text-overflow:ellipsis; overflow: hidden;">${item.description}</td>
            </tr>
        </#list>
        <tr>
            <td colspan="7" style="font-size: 18px; line-height: 24px; color: red; text-align: center; font-weight: bold;">使用表格指令即可调用机器人功能</td>
        </tr>
    </table>
</div>
</body>
</html>
