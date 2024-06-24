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
        <#list list as item>
        <thead>
            <tr>
                <td colspan="12" style="font-size: 22px; font-weight: bold; text-align: center;">${item.name}</td>
            </tr>
        </thead>
        <tbody>
            <#list item.list as action>
            <#if action?index % 4 == 0>
            <tr>
            </#if>
                <td colspan="3">
                    <span style="font-size: 18px">${action.cmd}</span>
                    <br />
                    <span style="font-size: 12px; white-space: nowrap; text-overflow:ellipsis; overflow: hidden; width: 240px; display: block">
                        ${action.description}
                    </span>
                </td>
            <#if action?index % 4 == 3>
            </tr>
            </#if>
            </#list>
            </tr>
        </tbody>
        </#list>
        <tfoot>
            <tr>
                <td colspan="12" style="font-size: 18px; line-height: 24px; color: red; text-align: center; font-weight: bold;">使用表格指令即可调用机器人功能</td>
            </tr>
        </tfoot>
    </table>
</div>
</body>
</html>
