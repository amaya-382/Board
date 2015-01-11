$(function () {
    var $spans_id = $("#label_id").children("span");
    $("#id").change(function () {
        var $self = $(this);
        $.get("/board/isValidId/", $(this).val(), function (res) {
            if (!!res) {
                $spans_id.first().hide();
                $spans_id.last().show();
                $self.addClass("underline_green");
                $self.removeClass("underline_red");
            } else {
                $spans_id.first().show();
                $spans_id.last().hide();
                $self.addClass("underline_red");
                $self.removeClass("underline_green");
            }
        });
    });

    var $spans_pwd = $("#label_reinput").children("span");
    var $password = $("#password");
    $("#reinput").keyup(function () {
        if ($(this).val() === $password.val()) {
            $spans_pwd.first().hide();
            $spans_pwd.last().show();
            $(this).addClass("underline_green");
            $password.addClass("underline_green");
            $password.removeClass("underline_red");
            $(this).removeClass("underline_red");
        } else {
            $spans_pwd.first().show();
            $spans_pwd.last().hide();
            $(this).addClass("underline_red");
            $password.addClass("underline_red");
            $password.removeClass("underline_green");
            $(this).removeClass("underline_green");
        }
    });
});
