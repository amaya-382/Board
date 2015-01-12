$(function () {
    $("#button_change_settings").click(function () {
        if (!window.confirm("設定を更新します. よろしいですか？"))
            return;

        var $self = $(this);
        var $name = $("#name");
        var $login_user_name = $("#login_user_name");
        $.ajax({
            "url": "/board/changeSettings",
            "method": "POST",
            "data": {
                "name": $name.val(),
                "token": $login_user_name.data("token")
            }
        }).done(function (res) {
            console.log(res);
            var dict = JSON.parse(res);
            $login_user_name.text(dict["name"]);
            $name.data("org", dict["name"]);
            $("input").css("color", "gray");
            $self.children("img").first().show();
        }).fail(function () {
            $self.children("img").last().show();
        });
    });

    $("input").keyup(function () {
        if ($(this).data("org") != $(this).val())
            $(this).css("color", "#311");
        else
            $(this).css("color", "gray");
    });
});
